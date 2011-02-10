#!/usr/bin/perl -w

# For sequence classification, this will take tab-separated data which has 
# only IO-annotation (O PERS PERS O O LOC LOC O) and turn it into 
# IOB1 annotation as used in CoNLL shared tasks and evaluated by conlleval.

# It expects input in 3 whitespace separated columns, giving, word,
# gold tag, and chosen tag.
# 
# HN: TODO: parameterize the column containing the guessed answers that needs
# to be split

my $nullTag = "O";

while( @ARGV && ($ARGV[0] =~ /^\-/) ) {
    if( $ARGV[0] =~ /^--nulltag/ || $ARGV[0] =~ /^-nt/ ) {
        shift @ARGV;
        $nullTag = shift @ARGV;
    }
}

$lastTag1 = $nullTag;
$lastTag2 = $nullTag;

while(<>) {
    chomp;
    @cols = split(/\s+/);
    if ($#cols > 1) {
	$tag1 = $cols[1];
        if( $tag1 eq $nullTag ) {
            $tag1 = "O";
        }
	$newTag1 = $tag1;
	$tag2 = $cols[2];
        if( $tag2 eq $nullTag ) {
            $tag2 = "O"
        }
	$newTag2 = $tag2;
	if ($tag1 ne "O") {
	    if ($tag1 eq $lastTag1) {
		$newTag1 = "I-".$tag1;
	    } else {
		$newTag1 = "B-".$tag1;
	    }
	}
	if ($tag2 ne "O") {
	    if ($tag2 eq $lastTag2) {
		$newTag2 = "I-".$tag2;
	    } else {
		$newTag2 = "B-".$tag2;
	    }
	}
	$lastTag1 = $tag1;
	$lastTag2 = $tag2;
	print "$cols[0]\t$newTag1\t$newTag2\n";
    } else {
	print "\n";
    }
}
