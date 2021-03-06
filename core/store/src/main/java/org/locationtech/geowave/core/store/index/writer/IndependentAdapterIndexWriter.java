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
package org.locationtech.geowave.core.store.index.writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.locationtech.geowave.core.index.ByteArrayId;
import org.locationtech.geowave.core.index.InsertionIds;
import org.locationtech.geowave.core.index.SinglePartitionInsertionIds;
import org.locationtech.geowave.core.store.IndexWriter;
import org.locationtech.geowave.core.store.adapter.IndexDependentDataAdapter;
import org.locationtech.geowave.core.store.data.VisibilityWriter;
import org.locationtech.geowave.core.store.index.PrimaryIndex;

public class IndependentAdapterIndexWriter<T> implements
		IndexWriter<T>
{

	final IndexDependentDataAdapter<T> adapter;
	final PrimaryIndex index;
	final IndexWriter<T> writer;

	public IndependentAdapterIndexWriter(
			IndexDependentDataAdapter<T> adapter,
			PrimaryIndex index,
			IndexWriter<T> writer ) {
		super();
		this.writer = writer;
		this.index = index;
		this.adapter = adapter;
	}

	@Override
	public InsertionIds write(
			final T entry,
			final VisibilityWriter<T> feldVisibilityWriter ) {
		final Iterator<T> indexedEntries = adapter.convertToIndex(
				index,
				entry);
		final List<SinglePartitionInsertionIds> partitionInsertionIds = new ArrayList<SinglePartitionInsertionIds>();
		while (indexedEntries.hasNext()) {
			InsertionIds ids = writer.write(
					indexedEntries.next(),
					feldVisibilityWriter);
			partitionInsertionIds.addAll(ids.getPartitionKeys());
		}
		return new InsertionIds(
				partitionInsertionIds);

	}

	@Override
	public void close()
			throws IOException {
		writer.close();
	}

	@Override
	public InsertionIds write(
			T entry ) {
		final Iterator<T> indexedEntries = adapter.convertToIndex(
				index,
				entry);
		final List<SinglePartitionInsertionIds> partitionInsertionIds = new ArrayList<SinglePartitionInsertionIds>();
		while (indexedEntries.hasNext()) {
			InsertionIds ids = writer.write(indexedEntries.next());
			partitionInsertionIds.addAll(ids.getPartitionKeys());
		}
		return new InsertionIds(
				partitionInsertionIds);
	}

	@Override
	public PrimaryIndex[] getIndices() {
		return writer.getIndices();
	}

	@Override
	public void flush() {
		writer.flush();
	}
}
