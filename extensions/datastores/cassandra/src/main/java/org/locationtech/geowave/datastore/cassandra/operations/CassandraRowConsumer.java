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
package org.locationtech.geowave.datastore.cassandra.operations;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraRowConsumer<T> implements
		Iterator<T>
{
	private final static Logger LOGGER = LoggerFactory.getLogger(CassandraRowConsumer.class);
	private Object nextRow = null;
	private final BlockingQueue<Object> blockingQueue;
	protected static final Object POISON = new Object();

	public CassandraRowConsumer(
			final BlockingQueue<Object> blockingQueue ) {
		this.blockingQueue = blockingQueue;
	}

	@Override
	public boolean hasNext() {
		if (nextRow != null) {
			return true;
		}
		else {
			try {
				nextRow = blockingQueue.take();
			}
			catch (final InterruptedException e) {
				LOGGER.warn(
						"Interrupted while waiting on hasNext",
						e);
				return false;
			}
		}
		if (!nextRow.equals(POISON)) {
			return true;
		}
		else {
			try {
				blockingQueue.put(POISON);
			}
			catch (final InterruptedException e) {
				LOGGER.warn(
						"Interrupted while finishing consuming from queue",
						e);
			}
			nextRow = null;
			return false;
		}
	}

	@Override
	public T next() {
		final T retVal = (T) nextRow;
		if (retVal == null) {
			throw new NoSuchElementException(
					"No more Cassandra rows");
		}
		nextRow = null;
		return retVal;
	}

}
