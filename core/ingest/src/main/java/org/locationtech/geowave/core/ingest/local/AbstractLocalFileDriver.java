/*******************************************************************************
 * Copyright (c) 2013-2018 Contributors to the Eclipse Foundation
 *   
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Apache License,
 *  Version 2.0 which accompanies this distribution and is available at
 *  http://www.apache.org/licenses/LICENSE-2.0.txt
 ******************************************************************************/
package org.locationtech.geowave.core.ingest.local;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.locationtech.geowave.core.cli.operations.config.options.ConfigOptions;
import org.locationtech.geowave.core.ingest.DataAdapterProvider;
import org.locationtech.geowave.core.ingest.IngestUtils;
import org.locationtech.geowave.core.ingest.IngestUtils.URLTYPE;
import org.locationtech.geowave.core.ingest.operations.ConfigAWSCommand;
import org.locationtech.geowave.core.store.cli.remote.options.IndexPluginOptions;
import org.locationtech.geowave.mapreduce.hdfs.HdfsUrlStreamHandlerFactory;
import org.locationtech.geowave.mapreduce.operations.ConfigHDFSCommand;
import org.locationtech.geowave.mapreduce.s3.S3URLStreamHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.upplication.s3fs.S3Path;

/**
 * This class can be sub-classed to handle recursing over a local directory
 * structure and passing along the plugin specific handling of any supported
 * file for a discovered plugin.
 * 
 * @param <P>
 *            The type of the plugin this driver supports.
 * @param <R>
 *            The type for intermediate data that can be used throughout the
 *            life of the process and is passed along for each call to process a
 *            file.
 */
abstract public class AbstractLocalFileDriver<P extends LocalPluginBase, R>
{
	private final static Logger LOGGER = LoggerFactory.getLogger(AbstractLocalFileDriver.class);
	protected LocalInputCommandLineOptions localInput;

	public AbstractLocalFileDriver(
			LocalInputCommandLineOptions input ) {
		localInput = input;
	}

	protected void processInput(
			final String inputPath,
			final File configFile,
			final Map<String, P> localPlugins,
			final R runData )
			throws IOException {
		if (inputPath == null) {
			LOGGER.error("Unable to ingest data, base directory or file input not specified");
			return;
		}

		Path path = null;
		Properties configProperties = null;

		if (configFile != null && configFile.exists()) {
			configProperties = ConfigOptions.loadProperties(configFile);
		}

		// If input path is S3
		if (inputPath.startsWith("s3://")) {
			try {
				IngestUtils.setURLStreamHandlerFactory(URLTYPE.S3);
			}
			catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
				LOGGER.error(
						"Error in setting up S3URLStreamHandler Factory",
						e1);
				return;
			}

			if (configProperties == null) {
				LOGGER.error("Unable to load properties form " + configFile.getAbsolutePath());
				return;
			}

			String s3EndpointUrl = configProperties.getProperty(ConfigAWSCommand.AWS_S3_ENDPOINT_URL);
			if (s3EndpointUrl == null) {
				LOGGER.error("S3 endpoint URL is empty. Config using \"geowave config aws <s3 endpoint url>\"");
				return;
			}

			if (!s3EndpointUrl.contains("://")) {
				s3EndpointUrl = "s3://" + s3EndpointUrl;
			}

			path = IngestUtils.setupS3FileSystem(
					inputPath,
					s3EndpointUrl);

		}
		// If input path is HDFS
		else if (inputPath.startsWith("hdfs://")) {
			try {
				IngestUtils.setURLStreamHandlerFactory(URLTYPE.HDFS);
			}
			catch (final Error | NoSuchFieldException | SecurityException | IllegalArgumentException
					| IllegalAccessException e) {
				LOGGER.error(
						"Error in setStreamHandlerFactory for HDFS",
						e);
				return;
			}

			String hdfsFSUrl = ConfigHDFSCommand.getHdfsUrl(configProperties);

			String hdfsInputPath = inputPath.replaceFirst(
					"hdfs://",
					"/");

			try {

				URI uri = new URI(
						hdfsFSUrl + hdfsInputPath);
				// HP Fortify "Path Traversal" false positive
				// What Fortify considers "user input" comes only
				// from users with OS-level access anyway
				path = Paths.get(uri);
				if (!Files.exists(path)) {
					LOGGER.error("Input path " + inputPath + " does not exist");
					return;
				}
			}
			catch (URISyntaxException e) {
				LOGGER.error(
						"Unable to ingest data, Inavlid HDFS Path",
						e);
				return;
			}
		}

		else {
			final File f = new File(
					inputPath);
			if (!f.exists()) {
				LOGGER.error("Input file '" + f.getAbsolutePath() + "' does not exist");
				throw new IllegalArgumentException(
						inputPath + " does not exist");
			}
			path = Paths.get(inputPath);
		}

		for (final LocalPluginBase localPlugin : localPlugins.values()) {
			localPlugin.init(path.toUri().toURL());
		}

		Files.walkFileTree(
				path,
				new LocalPluginFileVisitor<P, R>(
						localPlugins,
						this,
						runData,
						localInput.getExtensions()));
	}

	abstract protected void processFile(
			final URL file,
			String typeName,
			P plugin,
			R runData )
			throws IOException;
}
