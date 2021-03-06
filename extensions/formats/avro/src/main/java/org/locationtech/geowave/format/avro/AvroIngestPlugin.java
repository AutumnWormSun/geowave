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
package org.locationtech.geowave.format.avro;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.specific.SpecificDatumReader;
import org.locationtech.geowave.adapter.vector.AvroFeatureUtils;
import org.locationtech.geowave.adapter.vector.FeatureDataAdapter;
import org.locationtech.geowave.adapter.vector.avro.AttributeValues;
import org.locationtech.geowave.adapter.vector.avro.AvroSimpleFeatureCollection;
import org.locationtech.geowave.adapter.vector.avro.FeatureDefinition;
import org.locationtech.geowave.adapter.vector.ingest.AbstractSimpleFeatureIngestPlugin;
import org.locationtech.geowave.core.geotime.store.dimension.GeometryWrapper;
import org.locationtech.geowave.core.geotime.store.dimension.Time;
import org.locationtech.geowave.core.index.ByteArrayId;
import org.locationtech.geowave.core.ingest.GeoWaveData;
import org.locationtech.geowave.core.ingest.IngestPluginBase;
import org.locationtech.geowave.core.ingest.hdfs.mapreduce.IngestWithMapper;
import org.locationtech.geowave.core.ingest.hdfs.mapreduce.IngestWithReducer;
import org.locationtech.geowave.core.store.CloseableIterator;
import org.locationtech.geowave.core.store.CloseableIterator.Wrapper;
import org.locationtech.geowave.core.store.index.CommonIndexValue;
import org.locationtech.geowave.core.store.index.PrimaryIndex;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin is used for ingesting any GPX formatted data from a local file
 * system into GeoWave as GeoTools' SimpleFeatures. It supports the default
 * configuration of spatial and spatial-temporal indices and it will support
 * wither directly ingesting GPX data from a local file system to GeoWave or to
 * stage the data in an intermediate format in HDFS and then to ingest it into
 * GeoWave using a map-reduce job. It supports OSM metadata.xml files if the
 * file is directly in the root base directory that is passed in command-line to
 * the ingest framework.
 */
public class AvroIngestPlugin extends
		AbstractSimpleFeatureIngestPlugin<AvroSimpleFeatureCollection>
{

	private final static Logger LOGGER = LoggerFactory.getLogger(AvroIngestPlugin.class);

	public AvroIngestPlugin() {}

	@Override
	public String[] getFileExtensionFilters() {
		return new String[] {
			"avro",
			"dat",
			"bin",
			"json" // TODO: does the Avro DataFileReader actually support JSON
					// formatted avro files, or should we limit the extensions
					// to expected binary extensions?
		};
	}

	@Override
	public void init(
			final URL baseDirectory ) {}

	@Override
	public boolean supportsFile(
			final URL file ) {

		try (DataFileStream<AvroSimpleFeatureCollection> ds = new DataFileStream<AvroSimpleFeatureCollection>(
				file.openStream(),
				new SpecificDatumReader<AvroSimpleFeatureCollection>())) {
			if (ds.getHeader() != null) {
				return true;
			}
		}
		catch (final IOException e) {
			// just log as info as this may not have been intended to be read as
			// avro vector data
			LOGGER.info(
					"Unable to read file as Avro vector data '" + file.getPath() + "'",
					e);
		}

		return false;
	}

	@Override
	protected SimpleFeatureType[] getTypes() {
		return new SimpleFeatureType[] {};
	}

	@Override
	public Schema getAvroSchema() {
		return AvroSimpleFeatureCollection.getClassSchema();
	}

	@Override
	public CloseableIterator<AvroSimpleFeatureCollection> toAvroObjects(
			final URL input ) {
		try {
			final DataFileStream<AvroSimpleFeatureCollection> reader = new DataFileStream<AvroSimpleFeatureCollection>(
					input.openStream(),
					new SpecificDatumReader<AvroSimpleFeatureCollection>());

			return new CloseableIterator<AvroSimpleFeatureCollection>() {

				@Override
				public boolean hasNext() {
					return reader.hasNext();
				}

				@Override
				public AvroSimpleFeatureCollection next() {
					return reader.next();
				}

				@Override
				public void close()
						throws IOException {
					reader.close();
				}

			};
		}
		catch (final IOException e) {
			LOGGER.warn(
					"Unable to read file '" + input.getPath() + "' as AVRO SimpleFeatureCollection",
					e);
		}
		return new CloseableIterator.Empty<AvroSimpleFeatureCollection>();
	}

	@Override
	public boolean isUseReducerPreferred() {
		return false;
	}

	@Override
	public IngestWithMapper<AvroSimpleFeatureCollection, SimpleFeature> ingestWithMapper() {
		return new IngestAvroFeaturesFromHdfs(
				this);
	}

	@Override
	public IngestWithReducer<AvroSimpleFeatureCollection, ?, ?, SimpleFeature> ingestWithReducer() {
		// unsupported right now
		throw new UnsupportedOperationException(
				"Avro simple feature collections cannot be ingested with a reducer");
	}

	@Override
	protected CloseableIterator<GeoWaveData<SimpleFeature>> toGeoWaveDataInternal(
			final AvroSimpleFeatureCollection featureCollection,
			final Collection<ByteArrayId> primaryIndexIds,
			final String globalVisibility ) {
		final FeatureDefinition featureDefinition = featureCollection.getFeatureType();
		final List<GeoWaveData<SimpleFeature>> retVal = new ArrayList<GeoWaveData<SimpleFeature>>();
		SimpleFeatureType featureType;
		try {
			featureType = AvroFeatureUtils.avroFeatureDefinitionToGTSimpleFeatureType(featureDefinition);

			final FeatureDataAdapter adapter = new FeatureDataAdapter(
					featureType);
			final List<String> attributeTypes = featureDefinition.getAttributeTypes();
			for (final AttributeValues attributeValues : featureCollection.getSimpleFeatureCollection()) {
				try {
					final SimpleFeature simpleFeature = AvroFeatureUtils.avroSimpleFeatureToGTSimpleFeature(
							featureType,
							attributeTypes,
							attributeValues);
					retVal.add(new GeoWaveData<SimpleFeature>(
							adapter,
							primaryIndexIds,
							simpleFeature));
				}
				catch (final Exception e) {
					LOGGER.warn(
							"Unable to read simple feature from Avro",
							e);
				}
			}
		}
		catch (final ClassNotFoundException e) {
			LOGGER.warn(
					"Unable to read simple feature type from Avro",
					e);
		}
		return new Wrapper<GeoWaveData<SimpleFeature>>(
				retVal.iterator());
	}

	@Override
	public PrimaryIndex[] getRequiredIndices() {
		return new PrimaryIndex[] {};
	}

	public static class IngestAvroFeaturesFromHdfs extends
			AbstractIngestSimpleFeatureWithMapper<AvroSimpleFeatureCollection>
	{
		public IngestAvroFeaturesFromHdfs() {
			this(
					new AvroIngestPlugin());
			// this constructor will be used when deserialized
		}

		public IngestAvroFeaturesFromHdfs(
				final AvroIngestPlugin parentPlugin ) {
			super(
					parentPlugin);
		}
	}

	@Override
	public IngestPluginBase<AvroSimpleFeatureCollection, SimpleFeature> getIngestWithAvroPlugin() {
		return new IngestAvroFeaturesFromHdfs(
				this);
	}

	@Override
	public Class<? extends CommonIndexValue>[] getSupportedIndexableTypes() {
		return new Class[] {
			GeometryWrapper.class,
			Time.class
		};
	}
}
