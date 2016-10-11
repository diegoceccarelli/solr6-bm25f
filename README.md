## BM25F

the BM25F ranking formula is an extension of the BM25 ranking formula, modified to work
on documents with several fields [1,2]

## BM25F for Solr

I wrote bm25f for the first time in 2010 when I was collaborating with [http://www.europeana.eu/portal/en](Europeana), and then upgraded it several times
(with help from [http://pro.europeana.eu/person/yorgos-mamakis](Yorgos Mamakis) to the newer versions
of Solr, but never submitted a patch (my bad, I was shy).
I upgraded the old code to the Solr 6 interface during the [https://sites.google.com/site/lucene4ir/home](Lucene for IR Hackathon)
and during the [http://www.meetup.com/it-IT/Apache-Lucene-Solr-London-User-Group/](London Lucene Solr Meetup Hackathon).

## TODO

  - Together with [https://github.com/deVIAntCoDE](Henry Cleland) we ported the bm25f ranking function for a single term query. The bm25f
  boolean query needs to be fixed (and tested). The code that still has to be fixed is commented in the repo;
  - Explanation can be improved (and in general all the code, some methods/variables are not used, finals can be added ... );
  - More unit tests can be added, adapting them from the old ones (available in the [https://github.com/europeana/contrib/tree/master/bm25f-ranking](old repo));
  - Improve documentation, again I had some documentation in the old repo.

If you work want to work on this feel free to reach me at my email address `diego [dot] ceccarelli [at] gmail [dot] com`

[1] https://en.wikipedia.org/wiki/Okapi_BM25
[2] https://arxiv.org/pdf/0911.5046v2
