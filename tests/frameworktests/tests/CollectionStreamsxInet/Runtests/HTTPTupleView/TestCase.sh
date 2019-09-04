#--variantList='noPart_slide_4 noPart_slide_20 noPart_tumbl_20 noPart_tumbl_4 \
#--             force_noPart_slide_20 noPart_slide_20_attr noPart_slide_20_supp \
#--             part1_slide_20  part2_slide_20  force_part2_slide_20 \
#--             npart1_slide_20 npart2_slide_20 force_npart2_slide_20 \
#--             npart2_1_slide_20 npart2_1A_slide_20'
#--exclusive=true

setCategory 'quick'

PREPS=(
	'myExplain'
	'copyAndMorphSpl'
	'splCompile'
)
STEPS=(
	'submitJob -P jettyPort=8080 -P iterations1=$(getTupleCount)'
	'checkJobNo'
	'waitForJobHealth'
	'mySend'
	'cancelJobAndLog'
	'linewisePatternMatchInterceptAndSuccess "$TT_evaluationFile" "" "${resultExpected[$TTRO_variantCase]}"'
	'checkLogsNoError2'
)

FINS='cancelJobAndLog'

myExplain() {
	case "$TTRO_variantCase" in
	noPart_slide_4)  echo "variant $TTRO_variantCase - HTTPTupleView one input no partition sliding window test 1 - 4 tuples";;
	noPart_slide_20) echo "variant $TTRO_variantCase - HTTPTupleView one input no partition sliding window test 1 - 20 tuples";;
	noPart_tumbl_4)  echo "variant $TTRO_variantCase - HTTPTupleView one input no partition tumbling window test 1 - 4 tuples";;
	noPart_tumbl_20) echo "variant $TTRO_variantCase - HTTPTupleView one input no partition tumbling window test 1 - 20 tuples";;
	force_noPart_slide_20) echo "variant $TTRO_variantCase - HTTPTupleView one input no partition sliding window forceClean parameter test 1 - 20 tuples";;
	noPart_slide_20_attr) echo "variant $TTRO_variantCase - HTTPTupleView one input no partition sliding window test 2 attribute query - 20 tuples";;
	noPart_slide_20_supp) echo "variant $TTRO_variantCase - HTTPTupleView one input no partition sliding window test 2 suppress query - 20 tuples";;
	part1_slide_20)  echo "variant $TTRO_variantCase - HTTPTupleView one input partition 1 key sliding window test 3 - 20 tuples";;
	part2_slide_20)  echo "variant $TTRO_variantCase - HTTPTupleView one input partition 2 key sliding window test 3 - 20 tuples";;
	force_part2_slide_20)  echo "variant $TTRO_variantCase - HTTPTupleView one input partition 2 key sliding window test with force an without a query - 20 tuples";;
	npart1_slide_20)  echo "variant $TTRO_variantCase - HTTPTupleView one input partition 1 key sliding window test with named partitions - 20 tuples";;
	npart2_slide_20)  echo "variant $TTRO_variantCase - HTTPTupleView one input partition 2 key sliding window test with named partitions - 20 tuples";;
	force_npart2_slide_20)  echo "variant $TTRO_variantCase - HTTPTupleView one input partition 2 key sliding window test with force an without a query - 20 tuples";;
	npart2_1_slide_20)  echo "variant $TTRO_variantCase - HTTPTupleView one input partition 2 key sliding window test with named 1 partition query - 20 tuples";;
	npart2_1A_slide_20)  echo "variant $TTRO_variantCase - HTTPTupleView one input partition 2 key sliding window test with named 1 partition query and attribute query - 20 tuples";;
	*) printErrorAndExit "invalid variant $TTRO_variantCase";;
	esac
}

getTupleCount() {
	case "$TTRO_variantCase" in
	*_4*) echo "4";;
	*_20*) echo "20";;
	esac
}

mySend() {
	case "$TTRO_variantCase" in
	*_attr) executeLogAndSuccess curl -f 'http://localhost:8080/Sink/ports/input/0/tuples?attribute=c_int32&attribute=d_float';;
	*_supp) executeLogAndSuccess curl -f 'http://localhost:8080/Sink/ports/input/0/tuples?suppress=a_string&suppress=b_string';;
	part1_*) executeLogAndSuccess curl -f 'http://localhost:8080/Sink/ports/input/0/tuples?partition=1';;
	part2_*) executeLogAndSuccess curl -f 'http://localhost:8080/Sink/ports/input/0/tuples?partition=1&partition=false';;
	npart1_slide*) executeLogAndSuccess curl -f 'http://localhost:8080/Sink/ports/input/0/tuples?c_int32=1';;
	npart2_slide*) executeLogAndSuccess curl -f 'http://localhost:8080/Sink/ports/input/0/tuples?e_boolean=false&c_int32=1';;
	force_part*)executeLogAndSuccess curl -f 'http://localhost:8080/Sink/ports/input/0/tuples?attribute=a_string';;
	force_npart*)executeLogAndSuccess curl -f 'http://localhost:8080/Sink/ports/input/0/tuples?attribute=a_string';;
	*_1_*)executeLogAndSuccess curl -f 'http://localhost:8080/Sink/ports/input/0/tuples?e_boolean=false';;
	*_1A_*)executeLogAndSuccess curl -f 'http://localhost:8080/Sink/ports/input/0/tuples?e_boolean=false&attribute=e_boolean&attribute=d_float';;
	*)      executeLogAndSuccess curl -f 'http://localhost:8080/Sink/ports/input/0/tuples';;
	esac
}

declare -A resultExpected=( \
	[noPart_slide_4]='\[{"a_string":"rstr_0","b_string":"ustr_0","c_int32":0,"d_float":0.0,"e_boolean":false},{"a_string":"rstr_1","b_string":"ustr_1","c_int32":1,"d_float":1.0,"e_boolean":true},{"a_string":"rstr_2","b_string":"ustr_2","c_int32":2,"d_float":2.0,"e_boolean":true},{"a_string":"rstr_3","b_string":"ustr_3","c_int32":3,"d_float":3.0,"e_boolean":false}\]' \
	[noPart_slide_20]='\[{"a_string":"rstr_4","b_string":"ustr_4","c_int32":0,"d_float":4.0,"e_boolean":true},*,{"a_string":"rstr_19","b_string":"ustr_19","c_int32":3,"d_float":19.0,"e_boolean":true}\]' \
	[noPart_tumbl_4]='\[\]' \
	[noPart_tumbl_20]='\[{"a_string":"rstr_0","b_string":"ustr_0","c_int32":0,"d_float":0.0,"e_boolean":false},*,{"a_string":"rstr_15","b_string":"ustr_15","c_int32":3,"d_float":15.0,"e_boolean":false}\]' \
	[force_noPart_slide_20]='\[{"a_string":"rstr_4","b_string":"ustr_4","c_int32":0,"d_float":4.0,"e_boolean":true},*,{"a_string":"rstr_19","b_string":"ustr_19","c_int32":3,"d_float":19.0,"e_boolean":true}\]' \
	[noPart_slide_20_attr]='\[{"c_int32":0,"d_float":4.0},*,{"c_int32":3,"d_float":19.0}\]' \
	[noPart_slide_20_supp]='\[{"c_int32":0,"d_float":4.0,"e_boolean":true},*,{"c_int32":3,"d_float":19.0,"e_boolean":true}\]' \
	[part1_slide_20]='\[{"a_string":"rstr_1","b_string":"ustr_1","c_int32":1,"d_float":1.0,"e_boolean":true},{"a_string":"rstr_5","b_string":"ustr_5","c_int32":1,"d_float":5.0,"e_boolean":true},{"a_string":"rstr_9","b_string":"ustr_9","c_int32":1,"d_float":9.0,"e_boolean":false},{"a_string":"rstr_13","b_string":"ustr_13","c_int32":1,"d_float":13.0,"e_boolean":true},{"a_string":"rstr_17","b_string":"ustr_17","c_int32":1,"d_float":17.0,"e_boolean":true}\]' \
	[part2_slide_20]='\[{"a_string":"rstr_9","b_string":"ustr_9","c_int32":1,"d_float":9.0,"e_boolean":false}\]' \
	[force_part2_slide_20]='\[\]' \
	[npart1_slide_20]='\[{"a_string":"rstr_1","b_string":"ustr_1","c_int32":1,"d_float":1.0,"e_boolean":true},{"a_string":"rstr_5","b_string":"ustr_5","c_int32":1,"d_float":5.0,"e_boolean":true},{"a_string":"rstr_9","b_string":"ustr_9","c_int32":1,"d_float":9.0,"e_boolean":false},{"a_string":"rstr_13","b_string":"ustr_13","c_int32":1,"d_float":13.0,"e_boolean":true},{"a_string":"rstr_17","b_string":"ustr_17","c_int32":1,"d_float":17.0,"e_boolean":true}\]' \
	[npart2_slide_20]='\[{"a_string":"rstr_9","b_string":"ustr_9","c_int32":1,"d_float":9.0,"e_boolean":false}\]' \
	[force_npart2_slide_20]='\[\]' \
	[npart2_1_slide_20]='\[{"a_string":"rstr_3","b_string":"ustr_3","c_int32":3,"d_float":3.0,"e_boolean":false},{"a_string":"rstr_15","b_string":"ustr_15","c_int32":3,"d_float":15.0,"e_boolean":false},{"a_string":"rstr_6","b_string":"ustr_6","c_int32":2,"d_float":6.0,"e_boolean":false},{"a_string":"rstr_18","b_string":"ustr_18","c_int32":2,"d_float":18.0,"e_boolean":false},{"a_string":"rstr_9","b_string":"ustr_9","c_int32":1,"d_float":9.0,"e_boolean":false},{"a_string":"rstr_0","b_string":"ustr_0","c_int32":0,"d_float":0.0,"e_boolean":false},{"a_string":"rstr_12","b_string":"ustr_12","c_int32":0,"d_float":12.0,"e_boolean":false}\]' \
	[npart2_1A_slide_20]='\[{"e_boolean":false,"d_float":3.0},{"e_boolean":false,"d_float":15.0},{"e_boolean":false,"d_float":6.0},{"e_boolean":false,"d_float":18.0},{"e_boolean":false,"d_float":9.0},{"e_boolean":false,"d_float":0.0},{"e_boolean":false,"d_float":12.0}\]' \
)