#!/usr/bin/perl

use strict;
use open qw/:std :utf8/;

use Lingua::StanfordCoreNLP;
 
# Create a new NLP pipeline (make corefs bidirectional)
my $pipeline = new Lingua::StanfordCoreNLP::Pipeline(1);
 
# Get annotator properties:
my $props = $pipeline->getProperties();
 
# These are the default annotator properties:
$props->put('annotators', 'tokenize, ssplit, pos, lemma');
 
# Update properties:
$pipeline->setProperties($props);
 
while(<>)
{
  chomp;
  my $processed = $pipeline->process($_);
  for my $sentence(@{$processed->toArray})
  {
    for my $token (@{$sentence->getTokens->toArray})
    {
    	my $currword = lc($token->getLemma);
	# TODO: fix settings so we don't need these
	# 	replacements in the first place.
    	$currword =~ s/-lrb-/(/g;
    	$currword =~ s/-rrb-/)/g;
    	$currword =~ s/(\p{L})(\P{L})$/$1 $2/; 
		print "$currword ";
    }
  }
  print "\n";
}
