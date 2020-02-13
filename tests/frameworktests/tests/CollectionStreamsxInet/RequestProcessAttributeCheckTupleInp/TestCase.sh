#--variantCount=11

function myExplain {
	case "$TTRO_variantCase" in
	0) echo "variant $TTRO_variantCase - Tuple Mode Input port has no default attribute key";;
	1) echo "variant $TTRO_variantCase - Tuple Mode Input port key attribute with wrong type";;
	2) echo "variant $TTRO_variantCase - Tuple Mode Input port named key attribute is not available";;

	3) echo "variant $TTRO_variantCase - Tuple Mode Input port contentType attribute with wrong type";;
	4) echo "variant $TTRO_variantCase - Tuple Mode Input port named contentType attribute is not available";;
	5) echo "variant $TTRO_variantCase - Tuple Mode Input port response attribute with wrong type";;
	6) echo "variant $TTRO_variantCase - Tuple Mode Input port named response attribute is not available";;
	7) echo "variant $TTRO_variantCase - Tuple Mode Input port responseStatus attribute with wrong type";;
	8) echo "variant $TTRO_variantCase - Tuple Mode Input port named responseStatus attribute is not available";;
	9) echo "variant $TTRO_variantCase - Tuple Mode Input port responseHeader attribute with wrong type";;
	10) echo "variant $TTRO_variantCase - Tuple Mode Input port named responseHeader attribute is not available";;
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
	"*CDIST3856E Could not detect required attribute 'key' on input port 0. Or specify a valid value for parameter 'keyAttributeName'*"
	"*CDIST3857E Only type 'INT64' is allowed for attribute 'key'*"
	"*CDIST3856E Could not detect required attribute 'myKey' on input port 0. Or specify a valid value for parameter 'keyAttributeName'*"

	"*CDIST3858E Only types 'USTRING' and 'RSTRING' is allowed for attribute 'contentType'*"
	"*CDIST3856E Could not detect required attribute 'myContentType' on input port 0. Or specify a valid value for parameter 'responseContentTypeAttributeName'*"

	"*CDIST3858E Only types 'USTRING' and 'RSTRING' is allowed for attribute 'response'*"
	"*CDIST3856E Could not detect required attribute 'myResponse' on input port 0. Or specify a valid value for parameter 'responseAttributeName'*"

	"*CDIST3857E Only type 'INT32' is allowed for attribute 'status'*"
	"*CDIST3856E Could not detect required attribute 'myStatus' on input port 0. Or specify a valid value for parameter 'responseStatusAttributeName'*"

	"*CDIST3857E Only type 'MAP' is allowed for attribute 'header'*"
	"*CDIST3856E Could not detect required attribute 'myHeader' on input port 0. Or specify a valid value for parameter 'responseHeaderAttributeName'*"
)
