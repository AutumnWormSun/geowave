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
package org.locationtech.geowave.core.store.cli.remote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.geowave.core.cli.annotations.GeowaveOperation;
import org.locationtech.geowave.core.cli.api.OperationParams;
import org.locationtech.geowave.core.store.CloseableIterator;
import org.locationtech.geowave.core.store.DataStore;
import org.locationtech.geowave.core.store.DataStoreStatisticsProvider;
import org.locationtech.geowave.core.store.adapter.AdapterIndexMappingStore;
import org.locationtech.geowave.core.store.adapter.InternalDataAdapter;
import org.locationtech.geowave.core.store.adapter.statistics.StatsCompositionTool;
import org.locationtech.geowave.core.store.cli.remote.options.DataStorePluginOptions;
import org.locationtech.geowave.core.store.cli.remote.options.StatsCommandLineOptions;
import org.locationtech.geowave.core.store.index.IndexStore;
import org.locationtech.geowave.core.store.index.PrimaryIndex;
import org.locationtech.geowave.core.store.query.Query;
import org.locationtech.geowave.core.store.query.QueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

@GeowaveOperation(name = "recalcstats", parentOperation = RemoteSection.class)
@Parameters(commandDescription = "Calculate the statistics of an existing GeoWave dataset")
public class RecalculateStatsCommand extends
		AbstractStatsCommand<Void>
{

	private static final Logger LOGGER = LoggerFactory.getLogger(RecalculateStatsCommand.class);
	@Parameter(names = {
		"--adapterId"
	}, description = "Optionally recalculate a single adapter's stats")
	private String adapterId = "";

	@Parameter(description = "<store name>")
	private List<String> parameters = new ArrayList<String>();

	@Override
	public void execute(
			final OperationParams params ) {
		computeResults(params);
	}

	@Override
	protected boolean performStatsCommand(
			final DataStorePluginOptions storeOptions,
			final InternalDataAdapter<?> adapter,
			final StatsCommandLineOptions statsOptions )
			throws IOException {

		try {

			final AdapterIndexMappingStore mappingStore = storeOptions.createAdapterIndexMappingStore();
			final DataStore dataStore = storeOptions.createDataStore();
			final IndexStore indexStore = storeOptions.createIndexStore();

			boolean isFirstTime = true;
			for (final PrimaryIndex index : mappingStore.getIndicesForAdapter(
					adapter.getInternalAdapterId()).getIndices(
					indexStore)) {

				@SuppressWarnings({
					"rawtypes",
					"unchecked"
				})
				final DataStoreStatisticsProvider provider = new DataStoreStatisticsProvider(
						adapter,
						index,
						isFirstTime);
				final String[] authorizations = getAuthorizations(statsOptions.getAuthorizations());

				try (StatsCompositionTool<?> statsTool = new StatsCompositionTool(
						provider,
						storeOptions.createDataStatisticsStore(),
						index,
						adapter)) {
					try (CloseableIterator<?> entryIt = dataStore.query(
							new QueryOptions(
									adapter,
									index,
									(Integer) null,
									statsTool,
									authorizations),
							(Query) null)) {
						while (entryIt.hasNext()) {
							entryIt.next();
						}
					}
				}
				isFirstTime = false;
			}

		}
		catch (final Exception ex) {
			LOGGER.error(
					"Error while writing statistics.",
					ex);
			return false;
		}

		return true;
	}

	public List<String> getParameters() {
		return parameters;
	}

	public void setParameters(
			final String storeName,
			final String adapterName ) {
		parameters = new ArrayList<String>();
		parameters.add(storeName);
		if (adapterName != null) {
			parameters.add(adapterName);
		}
	}

	@Override
	public Void computeResults(
			final OperationParams params ) {
		// Ensure we have all the required arguments
		if (parameters.size() < 1) {
			throw new ParameterException(
					"Requires arguments: <store name>");
		}
		if ((adapterId != null) && !adapterId.trim().isEmpty()) {
			parameters.add(adapterId);
		}
		super.run(
				params,
				parameters);
		return null;
	}
}
