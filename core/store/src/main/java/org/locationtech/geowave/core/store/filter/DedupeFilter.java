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
package org.locationtech.geowave.core.store.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.locationtech.geowave.core.index.ByteArrayId;
import org.locationtech.geowave.core.store.data.IndexedPersistenceEncoding;
import org.locationtech.geowave.core.store.index.CommonIndexModel;

/**
 * This filter will perform de-duplication using the combination of data adapter
 * ID and data ID to determine uniqueness. It can be performed client-side
 * and/or distributed.
 * 
 */
public class DedupeFilter implements
		DistributableQueryFilter
{
	private final Map<Short, Set<ByteArrayId>> adapterIdToVisitedDataIdMap;

	private boolean dedupAcrossIndices = false;

	public DedupeFilter() {
		adapterIdToVisitedDataIdMap = new HashMap<Short, Set<ByteArrayId>>();
	}

	@Override
	public boolean accept(
			final CommonIndexModel indexModel,
			final IndexedPersistenceEncoding<?> persistenceEncoding ) {
		if (!persistenceEncoding.isDeduplicationEnabled()) {
			// certain types of data such as raster do not intend to be
			// duplicated
			// short circuit this check if the row is does not support
			// deduplication
			return true;
		}
		if (!isDedupAcrossIndices() && !persistenceEncoding.isDuplicated()) {
			// short circuit this check if the row is not duplicated anywhere
			// and this is only intended to support a single index
			return true;
		}
		final short adapterId = persistenceEncoding.getInternalAdapterId();
		final ByteArrayId dataId = persistenceEncoding.getDataId();
		synchronized (adapterIdToVisitedDataIdMap) {
			Set<ByteArrayId> visitedDataIds = adapterIdToVisitedDataIdMap.get(adapterId);
			if (visitedDataIds == null) {
				visitedDataIds = new HashSet<ByteArrayId>();
				adapterIdToVisitedDataIdMap.put(
						adapterId,
						visitedDataIds);
			}
			else if (visitedDataIds.contains(dataId)) {
				return false;
			}
			visitedDataIds.add(dataId);
			return true;
		}
	}

	public void setDedupAcrossIndices(
			boolean dedupAcrossIndices ) {
		this.dedupAcrossIndices = dedupAcrossIndices;
	}

	public boolean isDedupAcrossIndices() {
		return dedupAcrossIndices;
	}

	@Override
	public byte[] toBinary() {
		return new byte[] {};
	}

	@Override
	public void fromBinary(
			final byte[] bytes ) {}
}
