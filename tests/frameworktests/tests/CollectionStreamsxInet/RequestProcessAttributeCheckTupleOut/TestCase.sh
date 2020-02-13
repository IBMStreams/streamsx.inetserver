#--variantCount=17

function myExplain {
	case "$TTRO_variantCase" in
	0) echo "variant $TTRO_variantCase - Tuple Mode Output port has no default attribute key";;
	1) echo "variant $TTRO_variantCase - Tuple Mode Output port key attribute with wrong type";;
	2) echo "variant $TTRO_variantCase - Tuple Mode Output port named key attribute is not available";;

	3) echo "variant $TTRO_variantCase - Tuple Mode Output port contentType attribute with wrong type";;
	4) echo "variant $TTRO_variantCase - Tuple Mode Output port named contentType attribute is not available";;

	5) echo "variant $TTRO_variantCase - Tuple Mode Output port request attribute with wrong type";;
	6) echo "variant $TTRO_variantCase - Tuple Mode Output port named request attribute is not available";;

	7) echo "variant $TTRO_variantCase - Tuple Mode Output port header attribute with wrong type";;
	8) echo "variant $TTRO_variantCase - Tuple Mode Output port named header attribute is not available";;

	9) echo "variant $TTRO_variantCase - Tuple Mode Output port method attribute with wrong type";;
	10) echo "variant $TTRO_variantCase - Tuple Mode Output port named method attribute is not available";;

	11) echo "variant $TTRO_variantCase - Tuple Mode Output port pathInfo attribute with wrong type";;
	12) echo "variant $TTRO_variantCase - Tuple Mode Output port named pathInfo attribute is not available";;

	13) echo "variant $TTRO_variantCase - Tuple Mode Output port contextPath attribute with wrong type";;
	14) echo "variant $TTRO_variantCase - Tuple Mode Output port named contextPath attribute is not available";;

	15) echo "variant $TTRO_variantCase - Tuple Mode Output port contextPath url with wrong type";;
	16) echo "variant $TTRO_variantCase - Tuple Mode Output port named url attribute is not available";;
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
	"*CDIST3856E Could not detect required attribute 'key' on output port 0. Or specify a valid value for parameter 'keyAttributeName'*"
	"*CDIST3857E Only type 'INT64' is allowed for attribute 'key'*"
	"*CDIST3856E Could not detect required attribute 'myKey' on output port 0. Or specify a valid value for parameter 'keyAttributeName'*"

	"*CDIST3858E Only types 'USTRING' and 'RSTRING' is allowed for attribute 'contentType'*"
	"*CDIST3856E Could not detect required attribute 'myContentType' on output port 0. Or specify a valid value for parameter 'contentTypeAttributeName'*"

	"*CDIST3858E Only types 'USTRING' and 'RSTRING' is allowed for attribute 'request'*"
	"*CDIST3856E Could not detect required attribute 'myRequest' on output port 0. Or specify a valid value for parameter 'requestAttributeName'*"

	"*CDIST3857E Only type 'MAP' is allowed for attribute 'header'*"
	"*CDIST3856E Could not detect required attribute 'myHeader' on output port 0. Or specify a valid value for parameter 'headerAttributeName'*"

	"*CDIST3858E Only types 'USTRING' and 'RSTRING' is allowed for attribute 'method'*"
	"*CDIST3856E Could not detect required attribute 'myMethod' on output port 0. Or specify a valid value for parameter 'methodAttributeName'*"

	"*CDIST3858E Only types 'USTRING' and 'RSTRING' is allowed for attribute 'pathInfo'*"
	"*CDIST3856E Could not detect required attribute 'myPathInfo' on output port 0. Or specify a valid value for parameter 'pathInfoAttributeName'*"

	"*CDIST3858E Only types 'USTRING' and 'RSTRING' is allowed for attribute 'contextPath'*"
	"*CDIST3856E Could not detect required attribute 'myContextPath' on output port 0. Or specify a valid value for parameter 'contextPathAttributeName'*"

	"*CDIST3858E Only types 'USTRING' and 'RSTRING' is allowed for attribute 'url'*"
	"*CDIST3856E Could not detect required attribute 'myUrl' on output port 0. Or specify a valid value for parameter 'urlAttributeName'*"
)
