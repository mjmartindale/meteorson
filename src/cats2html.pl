#!perl
#
# Input:
# 	cats final file - meteorson text output
#
# Output:
#   HTML view of errors with original label available on hover
#
# Final error mapping:
#   miss, ext, lex - red
#   form  - green
#   syn, para   - blue
#   OOV   - fuchsia
#   reord - underline
#   
# Additional error flags:
#   _f    - bold
#   _c    - italic
 
use strict;
use open qw/:std :utf8/;
print "<html>\n<head><meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\"></meta> </head>\n";
print "<body>\n<font size=4>\n";
while(<>)
{
	my $catsline = $_;
	chomp($catsline);
	my($id,$empty,$refhyp,$text) = split /:/, $catsline;
	
	if($refhyp =~ /ref/){print "<p>\n${id}::REF:  "}
	elsif($refhyp =~ /hyp/){print "${id}::HYP:  "}
	else {next}
	
	my @words = split " ", $text;
	foreach my $element (@words)
	{
		my ($word,$labels) = split /~~/, $element;
		my $font = "";
		my $closefont = "</font>";
		my $hovertext = "<abbr title=\"$labels\">";
		my $hoverclose = "</abbr>";
		if ($labels =~ /miss/ || $labels =~ /ext/ || $labels =~ /lex/){ $font= "<font color=red>"}
		elsif($labels =~ /form/) {$font= "<font color=green>"}
		elsif($labels =~ /syn/ || $labels =~ /para/) {$font= "<font color=blue>"}
		elsif($labels =~ /OOV/i){$font= "<font color=fuchsia>"}
		else {$closefont = ""}
		
		my $bold = "";
		my $closebold = "</b>";
		if($labels =~ /_c$/){$bold = "<b>"; }
		else {$closebold = ""; }
		
		my $underline = "";
		my $closeunder = "</u>";
		if($labels =~ /reord/){$underline = "<u>"}
		else {$closeunder = ""}
		
		print "$font$bold$underline$hovertext$word$hoverclose$closeunder$closebold$closefont ";
	}
	if($refhyp =~ /hyp/){print "</p>\n"}
	else {print "<br>\n"}
}
print "</body>\n</html>";