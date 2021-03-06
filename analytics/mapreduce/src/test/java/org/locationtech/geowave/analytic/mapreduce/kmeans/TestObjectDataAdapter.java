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
package org.locationtech.geowave.analytic.mapreduce.kmeans;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.locationtech.geowave.core.geotime.store.dimension.GeometryWrapper;
import org.locationtech.geowave.core.index.ByteArrayId;
import org.locationtech.geowave.core.store.adapter.AbstractDataAdapter;
import org.locationtech.geowave.core.store.adapter.NativeFieldHandler;
import org.locationtech.geowave.core.store.adapter.PersistentIndexFieldHandler;
import org.locationtech.geowave.core.store.adapter.NativeFieldHandler.RowBuilder;
import org.locationtech.geowave.core.store.data.PersistentValue;
import org.locationtech.geowave.core.store.data.field.FieldReader;
import org.locationtech.geowave.core.store.data.field.FieldUtils;
import org.locationtech.geowave.core.store.data.field.FieldWriter;
import org.locationtech.geowave.core.store.dimension.NumericDimensionField;
import org.locationtech.geowave.core.store.index.CommonIndexModel;
import org.locationtech.geowave.core.store.index.CommonIndexValue;
import org.locationtech.geowave.core.store.index.PrimaryIndex;
import org.locationtech.geowave.mapreduce.HadoopDataAdapter;
import org.locationtech.geowave.mapreduce.HadoopWritableSerializer;

import com.vividsolutions.jts.geom.Geometry;

public class TestObjectDataAdapter extends
		AbstractDataAdapter<TestObject> implements
		HadoopDataAdapter<TestObject, TestObjectWritable>
{
	private static final ByteArrayId GEOM = new ByteArrayId(
			"myGeo");
	private static final ByteArrayId ID = new ByteArrayId(
			"myId");
	private static final ByteArrayId GROUP_ID = new ByteArrayId(
			"myGroupId");

	private static final PersistentIndexFieldHandler<TestObject, ? extends CommonIndexValue, Object> GEOM_FIELD_HANDLER = new PersistentIndexFieldHandler<TestObject, CommonIndexValue, Object>() {

		@Override
		public ByteArrayId[] getNativeFieldIds() {
			return new ByteArrayId[] {
				GEOM
			};
		}

		@Override
		public CommonIndexValue toIndexValue(
				final TestObject row ) {
			return new GeometryWrapper(
					row.geo,
					new byte[0]);
		}

		@Override
		public PersistentValue<Object>[] toNativeValues(
				final CommonIndexValue indexValue ) {
			return new PersistentValue[] {
				new PersistentValue<Object>(
						GEOM,
						((GeometryWrapper) indexValue).getGeometry())
			};
		}

		@Override
		public byte[] toBinary() {
			return new byte[0];
		}

		@Override
		public void fromBinary(
				final byte[] bytes ) {

		}
	};

	private static final NativeFieldHandler<TestObject, Object> ID_FIELD_HANDLER = new NativeFieldHandler<TestObject, Object>() {

		@Override
		public ByteArrayId getFieldId() {
			return ID;
		}

		@Override
		public Object getFieldValue(
				final TestObject row ) {
			return row.id;
		}
	};

	private static final NativeFieldHandler<TestObject, Object> GROUP_ID_FIELD_HANDLER = new NativeFieldHandler<TestObject, Object>() {

		@Override
		public ByteArrayId getFieldId() {
			return GROUP_ID;
		}

		@Override
		public Object getFieldValue(
				final TestObject row ) {
			return row.groupID;
		}
	};

	private static final List<NativeFieldHandler<TestObject, Object>> NATIVE_FIELD_HANDLER_LIST = new ArrayList<NativeFieldHandler<TestObject, Object>>();
	private static final List<PersistentIndexFieldHandler<TestObject, ? extends CommonIndexValue, Object>> COMMON_FIELD_HANDLER_LIST = new ArrayList<PersistentIndexFieldHandler<TestObject, ? extends CommonIndexValue, Object>>();
	static {
		COMMON_FIELD_HANDLER_LIST.add(GEOM_FIELD_HANDLER);
		NATIVE_FIELD_HANDLER_LIST.add(ID_FIELD_HANDLER);
		NATIVE_FIELD_HANDLER_LIST.add(GROUP_ID_FIELD_HANDLER);
	}

	public TestObjectDataAdapter() {
		super(
				COMMON_FIELD_HANDLER_LIST,
				NATIVE_FIELD_HANDLER_LIST);
	}

	@Override
	public ByteArrayId getAdapterId() {
		return new ByteArrayId(
				"test");
	}

	@Override
	public boolean isSupported(
			final TestObject entry ) {
		return true;
	}

	@Override
	public ByteArrayId getDataId(
			final TestObject entry ) {
		return new ByteArrayId(
				entry.id);
	}

	@Override
	public FieldReader getReader(
			final ByteArrayId fieldId ) {
		if (fieldId.equals(GEOM)) {
			return FieldUtils.getDefaultReaderForClass(Geometry.class);
		}
		else if (fieldId.equals(ID)) {
			return FieldUtils.getDefaultReaderForClass(String.class);
		}
		else if (fieldId.equals(GROUP_ID)) {
			return FieldUtils.getDefaultReaderForClass(String.class);
		}
		return null;
	}

	@Override
	public FieldWriter getWriter(
			final ByteArrayId fieldId ) {
		if (fieldId.equals(GEOM)) {
			return FieldUtils.getDefaultWriterForClass(Geometry.class);
		}
		else if (fieldId.equals(ID)) {
			return FieldUtils.getDefaultWriterForClass(String.class);
		}
		else if (fieldId.equals(GROUP_ID)) {
			return FieldUtils.getDefaultWriterForClass(String.class);
		}
		return null;
	}

	@Override
	protected RowBuilder newBuilder() {
		return new RowBuilder<TestObject, Object>() {
			private String id;
			private String groupID;
			private Geometry geom;

			@Override
			public void setField(
					ByteArrayId id,
					Object fieldValue ) {
				if (id.equals(GEOM)) {
					geom = (Geometry) fieldValue;
				}
				else if (id.equals(ID)) {
					this.id = (String) fieldValue;
				}
				else if (id.equals(GROUP_ID)) {
					groupID = (String) fieldValue;
				}
			}

			@Override
			public void setFields(
					Map<ByteArrayId, Object> values ) {
				if (values.containsKey(GEOM)) {
					geom = (Geometry) values.get(GEOM);
				}
				if (values.containsKey(ID)) {
					this.id = (String) values.get(ID);
				}
				if (values.containsKey(GROUP_ID)) {
					groupID = (String) values.get(GROUP_ID);
				}
			}

			@Override
			public TestObject buildRow(
					final ByteArrayId dataId ) {
				return new TestObject(
						geom,
						id,
						groupID);
			}
		};
	}

	@Override
	public HadoopWritableSerializer<TestObject, TestObjectWritable> createWritableSerializer() {
		return new TestObjectHadoopSerializer();
	}

	private class TestObjectHadoopSerializer implements
			HadoopWritableSerializer<TestObject, TestObjectWritable>
	{

		@Override
		public TestObjectWritable toWritable(
				final TestObject entry ) {
			return new TestObjectWritable(
					entry);
		}

		@Override
		public TestObject fromWritable(
				final TestObjectWritable writable ) {
			return writable.getObj();
		}

	}

	@Override
	public int getPositionOfOrderedField(
			final CommonIndexModel model,
			final ByteArrayId fieldId ) {
		int i = 0;
		for (final NumericDimensionField<? extends CommonIndexValue> dimensionField : model.getDimensions()) {
			if (fieldId.equals(dimensionField.getFieldId())) {
				return i;
			}
			i++;
		}
		if (fieldId.equals(GEOM)) {
			return i;
		}
		else if (fieldId.equals(ID)) {
			return i + 1;
		}
		else if (fieldId.equals(GROUP_ID)) {
			return i + 2;
		}
		return -1;
	}

	@Override
	public ByteArrayId getFieldIdForPosition(
			final CommonIndexModel model,
			final int position ) {
		if (position < model.getDimensions().length) {
			int i = 0;
			for (final NumericDimensionField<? extends CommonIndexValue> dimensionField : model.getDimensions()) {
				if (i == position) {
					return dimensionField.getFieldId();
				}
				i++;
			}
		}
		else {
			int numDimensions = model.getDimensions().length;
			if (position == numDimensions) {
				return GEOM;
			}
			else if (position == (numDimensions + 1)) {
				return ID;
			}
			else if (position == (numDimensions + 2)) {
				return GROUP_ID;
			}
		}
		return null;
	}

	@Override
	public void init(
			PrimaryIndex... indices ) {
		// TODO Auto-generated method stub

	}
}
