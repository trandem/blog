#!/usr/bin/env perl
# Phan tich message doc bang kafka-console-consumer (print.key=true, dong dang "key<TAB>value").
#   perl phan-tich.pl msgs FILE      : moi message mot dong: op, kich thuoc, before/after, gtid, do tre
#   perl phan-tich.pl decimal B64 S  : giai ma Decimal cua JsonConverter (base64, scale S)
#   perl phan-tich.pl pick FILE N    : in nguyen message thu N (dem tu 1)
use strict; use warnings;
use JSON::PP; use MIME::Base64;
my $json = JSON::PP->new->canonical(1);

sub dec { my ($b64, $scale) = @_; my $bytes = decode_base64($b64);
  my $n = 0; $n = $n * 256 + ord($_) for split //, $bytes;
  if (length($bytes) && ord(substr($bytes,0,1)) >= 128) { $n -= 256 ** length($bytes); }
  return ($n, sprintf("%.${scale}f", $n / 10 ** $scale)); }

# raw payload: phan van ban sau "payload": trong chuoi goc (khong serialize lai)
sub raw_payload { my ($s) = @_; my $i = index($s, '"payload":'); return undef if $i < 0;
  my $p = substr($s, $i + 10); $p =~ s/\}\s*$//; return $p; }

sub short { my ($h) = @_; return 'null' unless defined $h; return $json->encode($h); }

my $cmd = shift // '';
if ($cmd eq 'decimal') { my ($n, $v) = dec($ARGV[0], $ARGV[1] // 2); print "$ARGV[0] -> so nguyen $n, gia tri $v\n"; exit; }

my $file = shift or die "thieu FILE\n";
open my $fh, '<:raw', $file or die "$file: $!\n";
my @lines = grep { /\t/ } <$fh>; chomp @lines;

if ($cmd eq 'pick') { print $lines[$ARGV[0] - 1], "\n"; exit; }

my $from = shift // 1;   # msgs FILE [TU_MESSAGE_THU]
my $i = 0;
for my $l (@lines) {
  $i++;
  next if $i < $from;
  my ($k, $v) = split /\t/, $l, 2;
  my $kp = raw_payload($k); my $vp = defined $v ? raw_payload($v) : undef;
  my $line = sprintf "#%d key=%dB", $i, length($k);
  $line .= sprintf "(payload %dB %s)", length($kp), $kp if defined $kp;
  if (!defined $v || $v eq 'null') { print "$line value=null (tombstone)\n"; next; }
  my $d = eval { decode_json($v) }; if (!$d) { print "$line value=$v\n"; next; }
  my $p = exists $d->{payload} ? $d->{payload} : $d;
  $line .= sprintf " value=%dB", length($v);
  $line .= sprintf "(payload %dB, schema %.0f%%)", length($vp), 100 * (1 - length($vp) / length($v)) if defined $vp && exists $d->{schema};
  if (ref $p eq 'HASH' && exists $p->{op}) {
    my $s = $p->{source} // {};
    $line .= " op=$p->{op}";
    $line .= " snapshot=$s->{snapshot}" if defined $s->{snapshot};
    $line .= " gtid=$s->{gtid}" if defined $s->{gtid};
    $line .= " file=$s->{file} pos=$s->{pos}" if defined $s->{file};
    $line .= " lsn=$s->{lsn}" if defined $s->{lsn};
    $line .= sprintf " tre=%dms", $p->{ts_ms} - $s->{ts_ms} if defined $p->{ts_ms} && defined $s->{ts_ms};
    $line .= "\n    before=" . short($p->{before}) . "\n    after=" . short($p->{after});
  } else { $line .= " value=" . short($p); }
  print "$line\n";
}
