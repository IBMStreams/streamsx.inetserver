#--variantCount=12

function myExplain {
	case "$TTRO_variantCase" in
	0) echo "variant $TTRO_variantCase - HTTPTupleView no partiton input but non empty parameter partitionKey";;
	1) echo "variant $TTRO_variantCase - HTTPTupleView no partiton input but non empty parameter partitionBy";;
	2) echo "variant $TTRO_variantCase - HTTPTupleView partiton input but no parameter partitionKey or partitionBy";;
	3) echo "variant $TTRO_variantCase - HTTPTupleView partiton input but partitionBy with wrong cardinality";;
	4) echo "variant $TTRO_variantCase - HTTPTupleView partiton input but partitionKey with duplicate name";;
	5) echo "variant $TTRO_variantCase - HTTPTupleView partiton input but partitionKey with duplicate name";;
	6) echo "variant $TTRO_variantCase - HTTPTupleView partiton input but partitionBy with duplicate name";;
	7) echo "variant $TTRO_variantCase - HTTPTupleView 1 partiton input 1 none part input with partitionKey";;
	8) echo "variant $TTRO_variantCase - HTTPTupleView 1 partiton input 1 none part input with partitionBy";;
	9) echo "variant $TTRO_variantCase - HTTPTupleView 2 partiton inputs with partitionKey and attribute is wrong for port1";;
	10) echo "variant $TTRO_variantCase - HTTPTupleView 2 partiton inputs with partitionKey and attribute is wrong for port1";;
	11) echo "variant $TTRO_variantCase - HTTPTupleView 2 partiton inputs with partitionBy and attribute is wrong for port1";;
	*) printErrorAndExit "invalid variant $TTRO_variantCase";;
	esac
}

PREPS=(
	'myExplain'
	'copyAndMorphSpl'
)

STEPS=(
	"splCompile port=8080"
	'executeLogAndError output/bin/standalone -t 2'
	'linewisePatternMatchInterceptAndSuccess "$TT_evaluationFile" "" "${errorCodes[$TTRO_variantCase]}"'
)

errorCodes=(
	'*No Input port window is partitioned, but parameter partitionKey has a non empty value*'
	'*No Input port window is partitioned, but parameter partitionBy has non empty value*'
	'*Window is partitioned but has no partitonAttributeNames*'
	'*The cardinality of parameter partitionBy (2) must be equal the number of input ports (1)*'
	'*partition name: a_string is duplicate for input port 0*'
	'*partition name: a_string is duplicate for input port 0*'
	'*partition name: a_string is duplicate for input port 0*'
	'*Input port 1 is not partitioned but has none empty partition name*'
	'*Input port 1 is not partitioned but has none empty partition name*'
	'*Input port 1 has no attribute with name c_int32*'
	'*Input port 1 has no attribute with name c_int32*'
	'*Input port 1 has no attribute with name c_int32*'
)
