#--variantCount=5

function myExplain {
	case "$TTRO_variantCase" in
	0) echo "variant $TTRO_variantCase - Output port has single attribute with type string";;
	1) echo "variant $TTRO_variantCase - Output port has no named attribute myMessageAttribute";;
	2) echo "variant $TTRO_variantCase - Output port has named attribute messageAttribute with wrong type";;
	3) echo "variant $TTRO_variantCase - Output port has no named attribute mySenderIdAttribute";;
	4) echo "variant $TTRO_variantCase - Output port has named attribute senderIdAttribute with wrong type";;
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
	"*CDIST3866E Port Attribute message must be of type rstring, ustring or blob*"
	"*CDIST3856E Could not detect required attribute 'myMessageAttribute' on output port 0. Or specify a valid value for parameter 'messageAttributeName'*"
	"*CDIST3866E Port Attribute messageAttribute must be of type rstring, ustring or blob*"
	"*CDIST3856E Could not detect required attribute 'mySenderIdAttribute' on output port 0. Or specify a valid value for parameter 'senderIdAttributeName'*"
	"*CDIST3858E Only types 'rstring' and 'ustring' is allowed for attribute 'senderIdAttribute'*"
)
