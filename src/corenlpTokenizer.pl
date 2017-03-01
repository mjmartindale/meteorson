# !/usr/bin/perl


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
    	my $currword = lc($token->getWord);
	# TODO: Update settings so we don't have to do these subs
    	$currword =~ s/-lrb-/(/g;
    	$currword =~ s/-rrb-/)/g;
	# We want "Mr." to be able to match with "Mr" 
    	$currword =~ s/(\p{L})(\P{L})$/$1 $2/;
		print "$currword ";
    }
  }
  print "\n";
}
