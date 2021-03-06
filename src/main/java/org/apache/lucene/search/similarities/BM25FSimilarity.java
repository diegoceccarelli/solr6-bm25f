/**
 *  Copyright 2012 Diego Ceccarelli
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
 */
package org.apache.lucene.search.similarities;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.search.BM25FParameters;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SmallFloat;

/**
 * BM25FSimililarity implements the BM25F similarity function.
 * 
 * @author Diego Ceccarelli <diego.ceccarelli@isti.cnr.it>
 * 
 *         Created on Nov 15, 2012
 */
public class BM25FSimilarity extends Similarity {
	/**
	 * Logger for this class
	 */
//	private static final Logger logger = LoggerFactory
//			.getLogger(BM25FSimilarity.class);

	BM25FParameters params;
	Map<String, Float> boosts;
	Map<String, Float> lengthBoosts;
	float k1;

	public BM25FSimilarity() {
		// logger.info("no defaults");
		params = new BM25FParameters();
		boosts = params.getFieldWeights();
		lengthBoosts = params.getFieldLengthBoosts();
		k1 = params.getK1();
	}

	public void setBM25FParams(BM25FParameters bm25fparams) {
		params = bm25fparams;

		boosts = params.getFieldWeights();
		lengthBoosts = params.getFieldLengthBoosts();
		k1 = params.getK1();
	}

	public String[] getFields() {
		return params.getFields();
	}

	public BM25FSimilarity(BM25FParameters params) {
		// logger.info("defaults");
		this.params = params;
		boosts = params.getFieldWeights();
		lengthBoosts = params.getFieldLengthBoosts();
		k1 = params.getK1();
	}

	public BM25FSimilarity(float k1, Map<String, Float> boosts,
			Map<String, Float> lengthBoosts) {
		this.k1 = k1;
		this.boosts = boosts;
		this.lengthBoosts = lengthBoosts;
	}

	// Default true
	protected boolean discountOverlaps = true;

	/** @see #setDiscountOverlaps */
	public boolean getDiscountOverlaps() {
		return discountOverlaps;
	}

	/** Cache of decoded bytes. */
	private static final float[] NORM_TABLE = new float[256];

	// since lucene store the field lengths is a lossy format,
	// which is encoded in 1 byte (i.e., 256 different values).
	// the decoded values are stored in a cache.
	static {
		NORM_TABLE[0] = 0;
		for (int i = 1; i < 256; i++) {
			final float f = SmallFloat.byte315ToFloat((byte) i);

			NORM_TABLE[i] = 1.0f / (f * f);
		}
	}

	/**
	 * Determines whether overlap tokens (Tokens with 0 position increment) are
	 * ignored when computing norm. By default this is true, meaning overlap
	 * tokens do not count when computing norms.
	 */
	public void setDiscountOverlaps(boolean v) {
		discountOverlaps = v;
	}

	/**
	 * The default implementation encodes <code>boost / sqrt(length)</code> with
	 * {@link SmallFloat#floatToByte315(float)}. This is compatible with
	 * Lucene's default implementation. If you change this, then you should
	 * change {@link #decodeNormValue(byte)} to match.
	 */
	protected byte encodeNormValue(float boost, int fieldLength) {
		return SmallFloat
				.floatToByte315(boost / (float) Math.sqrt(fieldLength));
	}

	/**
	 * The default implementation returns <code>1 / f<sup>2</sup></code> where
	 * <code>f</code> is {@link SmallFloat#byte315ToFloat(byte)}.
	 */
	protected float decodeNormValue(byte b) {
		return NORM_TABLE[b & 0xFF];
	}

//	@Override
//	public final void computeNorm(FieldInvertState state, Norm norm) {
//		final int numTerms = discountOverlaps ? state.getLength()
//				- state.getNumOverlap() : state.getLength();
//		norm.setByte(encodeNormValue(state.getBoost(), numTerms));
//	}

	@Override
	public final SimScorer simScorer(SimWeight weight,
			LeafReaderContext context) throws IOException {
		final BM25FSimWeight w = (BM25FSimWeight) weight;

		return new BM25FSimScorer(w, context.reader().getNormValues(w.field));

	}

	
  @Override
  public SimWeight computeWeight(CollectionStatistics collectionStats, TermStatistics... termStats) {
	  // TODO Auto-generated method stub
//	  final Explanation idf = termStats.length == 1 ? idfExplain(collectionStats,
//				termStats[0]) : idfExplain(collectionStats, termStats);
//
	  float idf = idf(termStats[0].docFreq(), collectionStats.docCount());
	  boosts = params.getFieldWeights();
	  lengthBoosts = params.getFieldLengthBoosts();
	  k1 = params.getK1();

	  final String field = collectionStats.field();
	  final float avgdl = avgFieldLength(collectionStats);

	  // ignoring query boost, using bm25f query boost
	  float boost = 1;
	  if (boosts.containsKey(field)) {
		  boost = boosts.get(field);
	  }
	  float lengthBoost = 1;
	  if (lengthBoosts.containsKey(field)) {
		  lengthBoost = lengthBoosts.get(field);
	  }

	  // compute freq-independent part of bm25 equation across all norm values
	  float cache[] = new float[256];
	  for (int i = 0; i < cache.length; i++) {
		  cache[i] = ((1 - lengthBoost) + lengthBoost * decodeNormValue((byte) i) / avgdl);
//			   System.out.println("cache " + i + "\t" + cache[i]);
	  }

	  return new BM25FSimWeight(field, idf, boost, avgdl, null, k1);

  }


	/**
	 * Compute the average length for a field, given its stats.
	 * 
	 * @param stats the length statistics of a field.
	 * @return the average length of the field.
	 */
	private float avgFieldLength(CollectionStatistics stats) {
		// logger.info("sum total term freq \t {}", stats.sumTotalTermFreq());
		// logger.info("doc count \t {}", stats.docCount());
		return (float) stats.sumTotalTermFreq() / (float) stats.docCount();
	}

	/** Implemented as <code>1 / (distance + 1)</code>. */
	protected float sloppyFreq(int distance) {
		return 1.0f / (distance + 1);
	}

	/** The default implementation returns <code>1</code> */
	protected float scorePayload(int doc, int start, int end, BytesRef payload) {
		return 1;
	}



	/**
	 * Return the inverse document frequency (IDF), given the document frequency
	 * and the number of document in a collection. Implemented as
	 * 
	 * <code>log(1 + (numDocs - docFreq + 0.5)/(docFreq + 0.5))</code>.
	 * 
	 * @param numDocs
	 *            the number of documents in the index.
	 * @param docFreq
	 *            the number of documents containing the term
	 * @return the inverse document frequency.
	 * 
	 */
	protected float idf(long docFreq, long numDocs) {
		return (float) Math.log(1 + (((numDocs - docFreq) + 0.5D)
				/ (docFreq + 0.5D)));
	}

	/**
	 * @return the saturation parameter.
	 */
	public float getK1() {
		return k1;
	}

	public class BM25FSimScorer extends SimScorer {

		private final BM25FSimWeight stats;
		private final NumericDocValues norms;
		private final Map<String, Float> bParams;
		private final Map<String, Float> boosts;

		BM25FSimScorer(BM25FSimWeight stats, NumericDocValues norms)
				throws IOException {

			this.stats = stats;
			bParams = params.getFieldLengthBoosts();
			boosts = params.getFieldWeights();

			// this.cache = stats.cache;

			this.norms = norms;

		}

		@Override
		public Explanation explain(int doc, Explanation freq) {

			return explainScore(doc, freq, stats, norms);
		}

		@Override
		public float score(int doc, float freq) {
			String field = stats.getField();

			float fieldBoost = params.getBoost(field);
			float fieldLengthBoost = params.getLengthBoost(field);
			float fieldLength = decodeNormValue((byte)norms.get(doc));
			float fieldAverageLength = stats.avgdl;

			float nominator = freq * fieldBoost;
			float denominator = ((1 - fieldLengthBoost) + fieldLengthBoost * (fieldLength/ fieldAverageLength));
			return nominator / denominator;
		}



    @Override
    public float computePayloadFactor(int arg0, int arg1, int arg2,
        BytesRef arg3) {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public float computeSlopFactor(int arg0) {
      // TODO Auto-generated method stub
      return 0;
    }

  
	}

	public class BM25FSimWeight extends SimWeight {

		String field;
		float idf;
		float queryBoost;
		float avgdl;
		float cache[];
		float k1;

		float topLevelBoost;

		BM25FParameters params;

		/**
		 * @param field
		 * @param idf
		 * @param queryBoost
		 * @param avgdl
		 * @param k1
		 */
		public BM25FSimWeight(String field, float idf, float queryBoost, float avgdl, float cache[], float k1) {
			this.field = field;
			this.idf = idf;
			this.queryBoost = queryBoost;
			this.avgdl = avgdl;
			this.cache = cache;
			this.k1 = k1;

		}

		@Override
		public float getValueForNormalization() {
			// we return a TF-IDF like normalization to be nice, but we don't
			// actually normalize ourselves.
			final float queryWeight = idf * queryBoost;
			return queryWeight * queryWeight;
		}

		@Override
		public void normalize(float queryNorm, float topLevelBoost) {
			// we don't normalize with queryNorm at all, we just capture the
			// top-level boost
			// this.topLevelBoost = topLevelBoost;
			// this.weight = queryBoost * topLevelBoost;
		}

		public String getField() {
			return field;
		}

	}

  @Override
  public long computeNorm(FieldInvertState arg0) {
    // TODO Auto-generated method stub
    return 0;
  }




	private Explanation explainScore(int doc, Explanation freqExplain, BM25FSimWeight stats, NumericDocValues norms) {
		String field = stats.getField();
		float freq = freqExplain.getValue();
		float fieldWeight = params.getBoost(field);
		float fieldLengthWeight = params.getLengthBoost(field);
		float fieldLength = decodeNormValue((byte)norms.get(doc));
		float fieldAverageLength = stats.avgdl;

		Explanation boostExplain = Explanation.match(fieldWeight,"Field Boost:"+field);
		Explanation explainNumerator = Explanation.match(freq * fieldWeight,"Product of:",freqExplain,boostExplain);
		Explanation oneMinusBc = Explanation.match(1 - fieldLengthWeight, "1 - FieldLengthWeight["+field +"]");
		Explanation fieldLengthExplain = Explanation.match(fieldLength, "FieldLength");
		Explanation fieldAverageLengthExplain = Explanation.match(fieldAverageLength, "FieldAverageLength");
		Explanation lengthRatio = Explanation.match(fieldLength / fieldAverageLength, "Division of ", fieldLengthExplain, fieldAverageLengthExplain);

		Explanation lenghtBoostExplain = Explanation.match(fieldLengthWeight, "FieldLengthWeight");
		Explanation boostLengthRatio = Explanation.match(lengthRatio.getValue() * lenghtBoostExplain.getValue(), "Product of",lenghtBoostExplain, lengthRatio );
		Explanation denomExplain = Explanation.match(oneMinusBc.getValue() + boostLengthRatio.getValue(), "Sum of",oneMinusBc, boostLengthRatio);

		Explanation finalScore = Explanation.match(explainNumerator.getValue() / denomExplain.getValue() , "Division of" , explainNumerator, denomExplain);

		return  finalScore;

    }

}
