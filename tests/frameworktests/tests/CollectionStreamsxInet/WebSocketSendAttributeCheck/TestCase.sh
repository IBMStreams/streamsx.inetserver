#--variantCount=2

function myExplain {
	case "$TTRO_variantCase" in
	0) echo "variant $TTRO_variantCase - Input port has no named attribute myBinaryMessageAttribute";;
	1) echo "variant $TTRO_variantCase - Input port has attribute BinaryMessageAttribute with wrong type";;
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
	'*Could not detect required attribute "myBinaryMessageAttribute" on input port 0. Or specify a valid value for "binaryMessageAttributeName"*'
	"*CDIST0222E Only type 'BLOB' is allowed for attribute 'binaryMessageAttribute'*"
)
