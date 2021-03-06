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
package org.locationtech.geowave.core.store.query;

import java.util.Collections;
import java.util.List;

import org.locationtech.geowave.core.index.ByteArrayRange;
import org.locationtech.geowave.core.index.MultiDimensionalCoordinateRangesArray;
import org.locationtech.geowave.core.index.NumericIndexStrategy;
import org.locationtech.geowave.core.index.sfc.data.MultiDimensionalNumericData;
import org.locationtech.geowave.core.store.filter.DistributableQueryFilter;
import org.locationtech.geowave.core.store.filter.QueryFilter;
import org.locationtech.geowave.core.store.index.Index;
import org.locationtech.geowave.core.store.index.PrimaryIndex;
import org.locationtech.geowave.core.store.index.SecondaryIndex;

public class CoordinateRangeQuery implements
		DistributableQuery
{
	private NumericIndexStrategy indexStrategy;
	private MultiDimensionalCoordinateRangesArray[] coordinateRanges;

	public CoordinateRangeQuery() {}

	public CoordinateRangeQuery(
			final NumericIndexStrategy indexStrategy,
			final MultiDimensionalCoordinateRangesArray[] coordinateRanges ) {
		this.indexStrategy = indexStrategy;
		this.coordinateRanges = coordinateRanges;
	}

	@Override
	public List<QueryFilter> createFilters(
			final PrimaryIndex index ) {
		return Collections.singletonList(new CoordinateRangeQueryFilter(
				indexStrategy,
				coordinateRanges));
	}

	@Override
	public List<MultiDimensionalNumericData> getIndexConstraints(
			final PrimaryIndex index ) {
		// TODO should we consider implementing this?
		return Collections.EMPTY_LIST;
	}

	@Override
	public byte[] toBinary() {
		return new CoordinateRangeQueryFilter(
				indexStrategy,
				coordinateRanges).toBinary();
	}

	@Override
	public void fromBinary(
			final byte[] bytes ) {
		final CoordinateRangeQueryFilter filter = new CoordinateRangeQueryFilter();
		filter.fromBinary(bytes);
		indexStrategy = filter.indexStrategy;
		coordinateRanges = filter.coordinateRanges;
	}

	@Override
	public List<ByteArrayRange> getSecondaryIndexConstraints(
			final SecondaryIndex<?> index ) {
		// TODO should we consider implementing this?
		return null;
	}

	@Override
	public List<DistributableQueryFilter> getSecondaryQueryFilter(
			final SecondaryIndex<?> index ) {
		// TODO should we consider implementing this?
		return null;
	}
}
