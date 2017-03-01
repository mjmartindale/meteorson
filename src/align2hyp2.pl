#!/usr/bin/perl
#
# Reads in error categorized alignment and
# spits out a new version of the system output 
# for hjerson reordering labeling

use strict;
use open qw/:std :utf8/;


while(<>)
{
	chomp;
	my @words = split;
	foreach my $word (@words)
	{
		if($word =~ /algn-err-cats/){next}
		my ($labeled,$ref) = split /\|\|/, $word;
		$ref =~ s/_/ /g;
		my ($hyp,$label) = split /~~/,$labeled;
		$hyp =~ s/_/ /g;
		if($label =~ /para/)
		{
			my @refwords = split / /,$ref;
			my @hypwords = split / /, $hyp;
			
			if(@refwords > @hypwords)
			{
				for(my $i=0; $i < @refwords; $i++)
				{
					if($i >= @hypwords)
					{
						print "#!# ";
					}
					print "$refwords[$i] ";
				}
			}
			elsif(@hypwords > @refwords)
			{
				for(my $i = 0; $i < @hypwords; $i++)
				{
					if($i >= @refwords)
					{
						my $lastword = $refwords[@refwords-1];
						print "$lastword ";
					}
					else
					{
						print "$refwords[$i] ";
					}
				}
			}
			else { print $ref}
		}
		elsif($label =~ /infl/ or $label =~ /syn/ or $label =~ /OOV/){
			print $ref;
		}
		else { print $hyp }
		
		print " ";
	}
	print "\n";
}