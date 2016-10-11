/**
 * Copyright 2012 Diego Ceccarelli
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.similarities.BM25FSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.Similarity.SimScorer;

/**
 * A boolean query made by only one term. Documents are scored using the BM25F
 * ranking function.
 *
 * @author Diego Ceccarelli <diego.ceccarelli@isti.cnr.it>
 *         <p>
 *         Created on Nov 25, 2012
 */
public class BM25FBooleanTermQuery extends Query {
    private final Term term;
    private final int docFreq;
    private final TermContext perReaderTermState;
    private final BM25FParameters bm25fParams;

    // private String defaultField = SolrFields.getInstance().getDefaultField();
    private String[] fields;

    final class BM25FTermWeight extends Weight {
        private final Similarity similarity;
        private final Similarity.SimWeight[] stats;
        private final TermContext termStates;
        float idf;
        public float k1;
        private final String[] fields;
        private final BM25FParameters bm25fParams;
        private final String defaultField;
        private final int field = -1;

        protected float idf(long docFreq, long numDocs) {
            return (float) Math
                    .log(1 + (((numDocs - docFreq) + 0.5D) / (docFreq + 0.5D)));
        }

        public BM25FTermWeight(IndexSearcher searcher, TermContext termStates,
                               TermContext[] fieldTermStates, BM25FParameters bm25fParams)
                throws IOException {
            super(BM25FBooleanTermQuery.this);
            assert termStates != null : "TermContext must not be null";
            this.bm25fParams = bm25fParams;
            this.defaultField = bm25fParams.getMainField();
            this.termStates = termStates;

            this.similarity = searcher.getSimilarity(true); // FIXME
            if (this.similarity instanceof BM25FSimilarity) {

                ((BM25FSimilarity) this.similarity).setBM25FParams(bm25fParams);
            }

            this.k1 = bm25fParams.getK1();
            this.fields = bm25fParams.getFields();

            final String termField = term.field();
            if (termField.equals(defaultField)) {
                this.stats = new Similarity.SimWeight[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    final Term fieldTerm = new Term(fields[i], term.text());
                    // getFieldWeights is not used
                    this.stats[i] = similarity.computeWeight(
                            searcher.collectionStatistics(fieldTerm.field()),
                            searcher.termStatistics(fieldTerm, fieldTermStates[i]));
                }
            } else {
                int fieldPos = 0;
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].equals(termField)) {
                        fieldPos = i;
                        break;
                    }
                }

                final Term fieldTerm = new Term(fields[fieldPos], term.text());
                // getFieldWeights is not used
                this.stats = new Similarity.SimWeight[1];
                this.stats[0] = similarity.computeWeight(
                        searcher.collectionStatistics(fieldTerm.field()),
                        searcher.termStatistics(fieldTerm, fieldTermStates[fieldPos]));

            }

            // System.out.println("term field is " + term.field());
            final Term fieldTerm = new Term(term.field(), term.text());
            final TermStatistics termStat = searcher.termStatistics(fieldTerm,
                    termStates);
            final long df = termStat.docFreq();
            final long numDocs = searcher.getIndexReader().numDocs();
            idf = idf(df, numDocs);

        }

        @Override
        public String toString() {
            return "weight(" + BM25FTermWeight.this + ")";
        }

        @Override
        public float getValueForNormalization() {
            // return stats.getValueForNormalization();
            return 0;
        }

        @Override
        public void normalize(float queryNorm, float topLevelBoost) {
            // stats.normalize(queryNorm, topLevelBoost);
        }

        @Override
        public BM25FTermScorer scorer(LeafReaderContext context) throws IOException {
            assert termStates.topReaderContext == ReaderUtil
                    .getTopLevelContext(context) : "The top-reader used to create Weight ("
                    + termStates.topReaderContext
                    + ") is not the same as the current reader's top-reader ("
                    + ReaderUtil.getTopLevelContext(context);

            final SimScorer[] scorers = new SimScorer[stats.length];
            final PostingsEnum[] docsEnums = new PostingsEnum[stats.length];
            // TermsEnum termDocs = null;
            PostingsEnum docsEnum = null;
            if (stats.length == 1) {
                // termDocs = getTermsEnum(context, fields[i],i);
                docsEnum = context.reader().postings(new Term(fields[0], term.text()));
                if (docsEnum != null) {
                    scorers[0] = similarity.simScorer(stats[0], context);
                    docsEnums[0] = docsEnum;
                }
            } else {
                for (int i = 0; i < stats.length; i++) {
                    // termDocs = getTermsEnum(context, fields[i],i);
                    docsEnum = context.reader().postings(new Term(fields[i], term.text()));

                    if (docsEnum != null) {
                        scorers[i] = similarity.simScorer(stats[i], context);
                        docsEnums[i] = docsEnum;
                    }
                    // System.out.println("DOC ENUM "+i);
                    // while (docsEnum[i].nextDoc() != Scorer.NO_MORE_DOCS){
                    // System.out.println(" -> "+docsEnum[i].docID()+"("+docsEnum[i].freq()+")");
                    // }

                    // assert docsEnum[i] != null;

                }
            }
            // FIXME acceptDocs is null
            return new BM25FTermScorer(this, scorers, docsEnums, null);

        }

        private PostingsEnum getDocsEnum(LeafReaderContext context, String field)
                throws IOException {

            return context.reader().postings(new Term(field, term.text()));

        }

        @Override
        public Explanation explain(LeafReaderContext context, int doc) throws IOException {
            final SimScorer[] scorers = new SimScorer[stats.length];
            for (int i = 0; i < stats.length; i++) {
                scorers[i] = similarity.simScorer(stats[i], context);
            }
            BM25FTermScorer scorer = scorer(context);
            scorer.iterator().advance(doc);
            float acum = 0;
            final List<Explanation> sub = new ArrayList<>();
            for (int i = 0; i < stats.length; i++) {
                int freq = scorer.getFieldFreq(i);
                if (freq == 0) {
                    continue;
                }
                final Explanation freqExplanation = Explanation.match(freq, "tf in " + fields[i]);

                final Explanation scoreExplanation = scorers[i].explain(doc, freqExplanation);
                acum += scoreExplanation.getValue();
                sub.add(scoreExplanation);
            }
            final Explanation scores = Explanation.match(acum, "field scores, sum of:", sub);
            Explanation idfExplanation = Explanation.match(idf, "idf");
            Explanation k1Explanation = Explanation.match(k1, "k1");
            Explanation sum = Explanation.match(k1 + acum, "Sum of ", k1Explanation, scores);
            Explanation div = Explanation.match(acum / (k1 + acum), "Division Of", scores, sum);
            Explanation result = Explanation.match(idf * div.getValue(), "[docid:"+doc+"] Product Of", idfExplanation, div);

            return result;


        }

        @Override
        public void extractTerms(Set<Term> arg0) {
            // TODO Auto-generated method stub

        }

    }

    /**
     * Constructs a query for the term <code>t</code>.
     */
    public BM25FBooleanTermQuery(Term t, BM25FParameters bm25fParams) {
        this(t, -1, bm25fParams);
    }

    /**
     * Expert: constructs a TermQuery that will use the provided docFreq instead
     * of looking up the docFreq against the searcher.
     */
    public BM25FBooleanTermQuery(Term t, int docFreq, BM25FParameters bm25fParams) {
        term = t;
        this.docFreq = docFreq;
        perReaderTermState = null;
        this.bm25fParams = bm25fParams;
    }

    /**
     * Expert: constructs a TermQuery that will use the provided docFreq instead
     * of looking up the docFreq against the searcher.
     */
    public BM25FBooleanTermQuery(Term t, TermContext states,
                                 BM25FParameters bm25fParams) {
        assert states != null;
        term = t;
        docFreq = states.docFreq();
        perReaderTermState = states;
        this.bm25fParams = bm25fParams;
    }

    /**
     * Returns the term of this query.
     */
    public Term getTerm() {
        return term;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores)
            throws IOException {
        // if (term.field().equals(bm25fParams.getMainField())) {
        fields = bm25fParams.getFields();
// }

// else {
// fields = new String[] { term.field() };
// }

        final IndexReaderContext context = searcher.getTopReaderContext();
        final TermContext termState;
        if ((perReaderTermState == null)
                || (perReaderTermState.topReaderContext != context)) {
            // make TermQuery single-pass if we don't have a PRTS or if the
            // context differs!
            termState = TermContext.build(context, term); // cache term
            // lookups!
        } else {
            // PRTS was pre-build for this IS
            termState = this.perReaderTermState;
        }
        final TermContext[] fieldTermContext = new TermContext[fields.length];

        for (int i = 0; i < fields.length; i++) {
            final Term t = new Term(fields[i], term.text());

            fieldTermContext[i] = TermContext.build(context, t);
        }
        // FIXME we must not ignore the given docFreq - if set use the given value
        // (lie)
        // if (docFreq != -1) {
        // termState.(docFreq);
        // }

        return new BM25FTermWeight(searcher, termState, fieldTermContext, bm25fParams);
    }

    /**
     * Prints a user-readable version of this query.
     */
    @Override
    public String toString(String field) {
        final StringBuilder buffer = new StringBuilder();
        if (!term.field().equals(field)) {
            buffer.append(term.field());
            buffer.append(":");
        }
        buffer.append(term.text());
        // FIXME buffer.append(ToStringUtils.boost(getBoost()));
        return buffer.toString();
    }

    /**
     * Returns true iff <code>o</code> is equal to this.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BM25FBooleanTermQuery)) {
            return false;
        }
        final BM25FBooleanTermQuery other = (BM25FBooleanTermQuery) o;
        // FIXME
        // return (this.getBoost() == other.getBoost())
        // && this.term.equals(other.term);
        return this.term.equals(other.term);
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public int hashCode() {
        // FIXME
        //return Float.floatToIntBits(getBoost()) ^ term.hashCode();
        return term.hashCode();
    }

    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        return this;
    }

}
