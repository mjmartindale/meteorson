# Meteorson
MT error analysis with <a href="http://www.cs.cmu.edu/~alavie/METEOR/">Meteor</a> and <a href="https://github.com/cidermole/hjerson">Hjerson</a>.

# Usage
To run Meteorson, you will need source, reference, and hypothesis files for the data you wish to evaluate. The main pipeline script, errors-pipeline.sh, expects these files to have the same base name (e.g., news.tr-en) and the appropriate suffix (src, ref, hyp). Simply pass this base filename to the script, and it will automatically run through the process (storing files for intermediate stages in meteorson/work) and write two output files: an inline annotated text file (e.g., news.tr-en.cats.final) and a web page view (e.g., news.tr-en.cats.html). 

# Installation
Meteorson is <a href="https://github.com/nano5th/meteorson">available on GitHub</a>. Clone the repository and make sure that the dependencies below are set. You will then need to compile the METEOR error classifier add-on:

javac -cp $METEOR/meteor-1.5.jar src/ErrorCategorizer.java
 

# Dependencies
Meteorson relies on three external packages: the Perl interface to Stanford's CoreNLP (<a href="http://search.cpan.org/~kal/Lingua-StanfordCoreNLP-0.10/lib/Lingua/StanfordCoreNLP.pm">Lingua::StanfordCoreNLP</a>), <a href="http://www.cs.cmu.edu/~alavie/METEOR/">Meteor</a>, and <a href="https://github.com/cidermole/hjerson">Hjerson</a>. Hjerson and METEOR can be installed anywhere on the system as long as environment variables $METEOR and $HJERSON are set. As packaged, tokenization and lemmatization are performed by CoreNLP via Perl scripts, but another tokenizer and/or lemmatizer can be substituted by editing the pipeline script.
