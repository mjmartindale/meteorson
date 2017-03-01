#!perl
#
# Input:
# 	cats file - hjerson output
#   mcats file - meteor-based errors
#   cats2 file - hjerson second pass for reording labels
#
# Output:
#   Single set of hjerson-style error labels
#   1::ref-err-cats: word1~~err word2~~err ...
#   1::hyp-err-cats: word1~~err word2~~err ...
#
# Final error types:
#   miss  - word appearing in ref but not hyp
#   ext  - function word appearing in hyp but not ref
#   lex   - missing word and extra word that may match
#   form  - incorrect word form (e.g. wrong verb tense)
#   syn   - labeled by Meteor as synonym (based on WordNet)
#   para  - labeled by Meteor as part of a paraphrase (derived from PPDB?)
#   OOV   - untranslated word
#   reord - correct word out of place. NOTE: may be combined with other labels!
#   
# Additional error flags:
#   _f    - function word
#   _c    - non-function (probably content) word
 
use strict;
use open qw/:std :utf8/;

my $catsfile = $ARGV[0];
my $mcatsfile = $ARGV[1];
my $cats2file = $ARGV[2];   #   <-- maybe make this optional?

open CATS, $catsfile or die "Unable to open cats file '$catsfile'\n";
open MCATS, $mcatsfile or die "Unable to open mcats file '$mcatsfile'\n";
open CATS2, $cats2file or die "Unable to open cats2 file '$cats2file'\n";

while(<CATS>)
{
	my $catsline = $_;
	chomp($catsline);
	$catsline =~ s/(\d+)\s(\d+)/$1_$2/g;
	$catsline =~ s/(\d+)\s(\d+)/$1_$2/g;
	my @catswords = split ' ',$catsline;
	#print $catsline,"\n";
	
	my $mcatsline = <MCATS>;
	chomp($mcatsline);
	$mcatsline =~ s/(\d+)\s(\d+)/$1_$2/g;
	$mcatsline =~ s/(\d+)\s(\d+)/$1_$2/g;
	my @mcatswords = split ' ',$mcatsline;
	#print $mcatsline,"\n";
	
	my $cats2line = <CATS2>;
	chomp($cats2line);
	$cats2line =~ s/(\S+)~~\S+ #!#~~ext /$1/g;
	$cats2line =~ s/(\d+)\s(\d+)/$1_$2/g;
	$cats2line =~ s/(\d+)\s(\d+)/$1_$2/g;
	my @cats2words = split ' ',$cats2line;
	#print $cats2line,"\n";
	
	
	print "$catswords[0]";
	
	for(my $i=1; $i<@catswords;$i++)
	{
		my ($wordc,$labelc) = split /~~/, $catswords[$i];
		my ($wordm,$labelm) = split /~~/, $mcatswords[$i];
		($labelm, my $funcm) = split /_/, $labelm;
		my ($word2,$label2) = split /~~/, $cats2words[$i];
		
		#print "\tlabels: $wordc=$labelc, $wordm=$labelm, $word2=$label2\n";
		
		my $truelabel = "";
		if($labelm =~ /syn/ or $labelm =~ /para/ or $labelm =~ /OOV/)
		{
			$truelabel = $labelm;
		}
		elsif($labelc =~ /infl/ or $labelm =~ /infl/ or $label2 =~ /infl/)
		{
			$truelabel = "form";
		}
		#elsif($labelc =~ /lex/)
		#{
		#	$truelabel = $labelc;
		#}
		elsif($labelm =~ /ext/ or $labelm =~ /mis/)
		{
			$truelabel = $labelm;
		}
		elsif($labelc =~ /^x/ or $labelc =~ /reord/ or $labelm =~ /^x/ )
		{
			$truelabel = "x";
		}
		else { print "******************* Unrecognized labels! c($wordc)='$labelc' m($wordm)='$labelm' 2($word2)='$label2' **************************** "; }
		
		if($label2 =~ /reord/ )
		{
			$truelabel = "${truelabel}_reord";
		}
		if($funcm =~ /f/)
		{
			$truelabel = "${truelabel}_f";
		}
		else{ $truelabel = "${truelabel}_c"}
		
		print "$wordc~~$truelabel ";
	}
	print "\n";
}