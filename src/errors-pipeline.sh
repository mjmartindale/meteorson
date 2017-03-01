#!/bin//bash

if [ -z ${METEOR+x} ]; then export METEOR='meteor1.5';
if [ -z ${HJERSON+x} ]; then export HJERSON='hjerson';
if [ -z ${METEORSON+x} ]; then export METEORSON='.';
export scripts=$METEORSON/scripts
export workdir=$METEORSON/work
if [ ! -d "$workdir" ]; then
	mkdir $workdir
fi

for infile in $@
do
	echo "*** Processing $infile"
	# Split into src, ref, hyp
	cat $infile | awk -F'\t' '{print $3}' > $workdir/$infile.src
	cat $infile | awk -F'\t' '{print $4}' > $workdir/$infile.ref
	cat $infile | awk -F'\t' '{print $5}' > $workdir/$infile.hyp

	# Tokenize and lemmatize system output and reference
	echo "** Lemmatizing & tokenizing $infile"
	echo "    Lemmatizing system output: cat $workdir/$infile.hyp | perl scriptscorenlp2hjbase.pl > $workdir/$infile.hyp.base"
	cat $workdir/$infile.hyp | perl $scripts/corenlp2hjbase.pl > $workdir/$infile.hyp.base
	echo "    Tokenizing system output: cat $workdir/$infile.hyp | perl $scripts/corenlpTokenizer.pl > $workdir/$infile.hyp.temp"
	cat $workdir/$infile.hyp | perl $scripts/corenlpTokenizer.pl > $workdir/$infile.hyp.temp
	cp $workdir/$infile.hyp.temp $workdir/$infile.hyp
	echo "    Lemmatizing reference: cat $workdir/$infile.ref | perl $scripts/corenlp2hjbase.pl > $workdir/$infile.ref.base"
	cat $workdir/$infile.ref | perl $scripts/corenlp2hjbase.pl > $workdir/$infile.ref.base
	echo "    Tokenizing reference: cat $workdir/$infile.ref | perl $scripts/corenlpTokenizer.pl > $workdir/$infile.ref.temp"
	cat $workdir/$infile.ref | perl $scripts/corenlpTokenizer.pl > $workdir/$infile.ref.temp
	cp $workdir/$infile.ref.temp $workdir/$infile.ref

	# Run meteor error categorizer and clean output
	echo "** Running meteor error categorizer"
	echo "java -Xmx2G -cp $METEOR/meteor-1.5.jar:$METEORSON ErrorCategorizer $workdir/$infile.hyp $workdir/$infile.ref $workdir/$infile.src > $workdir/$infile.mcats"
	java -Xmx2G -cp $METEOR/meteor-1.5.jar:$METEORSON ErrorCategorizer $workdir/$infile.hyp $workdir/$infile.ref $workdir/$infile.src > $workdir/$infile.mcats
	grep 'err-cats' $workdir/$infile.mcats | grep -v 'algn-err-cats' > $workdir/$infile.mcats.clean

	# Run hjerson and clean output
	echo "** Running Hjerson first pass"
	echo "python $HJERSON/hjerson+.py -H $workdir/$infile.hyp -R $workdir/$infile.ref -c $workdir/$infile.cats -s $workdir/$infile.sent -B $workdir/$infile.ref.base -b $workdir/$infile.hyp.base"
	python $HJERSON/hjerson+.py -H $workdir/$infile.hyp -R $workdir/$infile.ref -c $workdir/$infile.cats -s $workdir/$infile.sent -B $workdir/$infile.ref.base -b $workdir/$infile.hyp.base
	grep -v '^$' $workdir/$infile.cats > $workdir/$infile.cats.clean

	echo "** Running hjerson against modified system output"
	# Create new hyp line just for reordering:
	grep 'algn-err-cats' $workdir/$infile.mcats > $workdir/$infile.mcats.align
	cat $workdir/$infile.mcats.align | perl $SCRIPTS/align2hyp2.pl > $workdir/$infile.hyp2

	# Run hjerson against this modified hyp for next level reordering tags
	python $HJERSON/hjerson+.py -H $workdir/$infile.hyp2 -R $workdir/$infile.ref -c $workdir/$infile.cats2
	grep -v '^$' $workdir/$infile.cats2 > $workdir/$infile.cats2.clean

	# Merge Meteor & Hjerson outputs
	echo "** Merging meteor & hjerson outputs to $workdir/$infile.cats.final"
	perl $scripts/mergecats.pl $workdir/$infile.cats.clean $workdir/$infile.mcats.clean $workdir/$infile.cats2.clean > $workdir/$infile.cats.final
done
