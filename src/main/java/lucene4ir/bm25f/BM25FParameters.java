/*
 *  Copyright 2008 Joaquin Perez-Iglesias
 *  Copyright 2010 Diego Ceccarelli
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package lucene4ir.bm25f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parameters needed to calculate the BM25F relevance score.
 * 
 * @author Diego Ceccarelli <diego.ceccarelli@isti.cnr.it>
 * @since 13/dec/2010
 */

public class BM25FParameters {

	public String mainField;

	private List<String> fields;
	/*
	 * fieldWeights on fields, you can boost more the match on a field rather than
	 * another
	 */
	private Map<String, Float> fieldWeights;
	/*
	 * fieldWeights on length, you can boost a record if a field length is similar to
	 * the average field length in the collection
	 */
	private Map<String, Float> fieldLengthBoosts;

	float k1 = 1;



	@Override
	public BM25FParameters clone() {
		BM25FParameters clone = new BM25FParameters();
		clone.setK1(k1);
		clone.fieldWeights = new HashMap<String, Float>(fieldWeights);
		clone.fieldLengthBoosts = new HashMap<String, Float>(fieldLengthBoosts);
		clone.fields = fields;
		clone.mainField = mainField;
		return clone;
	}

	public BM25FParameters() {
		// default params
		fieldWeights = new HashMap<String, Float>();
		fieldLengthBoosts = new HashMap<String, Float>();
		fields = new ArrayList<>();
	};

	public BM25FParameters addFieldParams(String field, float fieldLengthBoost, float fieldWeight){
		fieldLengthBoosts.put(field, fieldLengthBoost);
		fieldWeights.put(field, fieldWeight);
		fields.add(field);
		return this;
	}



	public float getBoost(String field) {
		return fieldWeights.get(field);
	}


	/**
	 * @return the fields
	 */
	public String[] getFields() {
		return fields.toArray(new String[fields.size()]);
	}

	/**
	 * @param fieldWeights
	 *            - the fieldWeights to set (see bm25f formula)
	 */
	public void setFieldWeights(Float[] fieldWeights) {
		this.fieldWeights = new HashMap<String, Float>();
		for (int i = 0; i < fields.size(); i++) {
			this.fieldWeights.put(fields.get(i), fieldWeights[i]);
		}
	}

	/**
	 * @return the fieldWeights (see bm25f formula)
	 */
	public Map<String, Float> getFieldWeights() {
		return fieldWeights;
	}

	/**
	 * @param fieldLengthBoosts
	 *            the fieldLengthBoosts to set (see bm25f formula)
	 */
	public void setFieldLengthBoosts(Float[] fieldLengthBoosts) {
		this.fieldLengthBoosts = new HashMap<String, Float>();
		for (int i = 0; i < fields.size(); i++) {
			this.fieldLengthBoosts.put(fields.get(i), fieldLengthBoosts[i]);
		}
	}

	/**
	 * @return the fieldLengthBoosts (see bm25f formula)
	 */
	public Map<String, Float> getFieldLengthBoosts() {
		return fieldLengthBoosts;
	}

	/**
	 * @return the k1
	 */
	public float getK1() {
		return k1;
	}

	/**
	 * @param k1
	 *            the k1 to set
	 */
	public void setK1(float k1) {
		this.k1 = k1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fieldLengthBoosts == null) ? 0 : fieldLengthBoosts.hashCode());
		result = prime * result + ((fieldWeights == null) ? 0 : fieldWeights.hashCode());
		result = prime * result + fields.hashCode();
		result = prime * result + Float.floatToIntBits(k1);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BM25FParameters other = (BM25FParameters) obj;
		if (fieldLengthBoosts == null) {
			if (other.fieldLengthBoosts != null)
				return false;
		} else if (!fieldLengthBoosts.equals(other.fieldLengthBoosts))
			return false;
		if (fieldWeights == null) {
			if (other.fieldWeights != null)
				return false;
		} else if (!fieldWeights.equals(other.fieldWeights))
			return false;
		// FIXME check this hashfunction
		if (Float.floatToIntBits(k1) != Float.floatToIntBits(other.k1))
			return false;
		return true;
	}

	public String getMainField() {
		return mainField;
	}

	public void setMainField(String mainField) {
		this.mainField = mainField;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BM25FParameters [fields=" + fields.toString()
				+ ", fieldWeights=" + fieldWeights + ", fieldLengthBoosts=" + fieldLengthBoosts + ", k1=" + k1
				+ "]";
	}

}
