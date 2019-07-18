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
	'*Could not detect required attribute "key" on output port 0. Or specify a valid value for "keyAttributeName"*'
	'*Only types "INT64" allowed for attribute "key"*'
	'*Could not detect required attribute "myKey" on output port 0. Or specify a valid value for "keyAttributeName"*'

	'*Only types "USTRING" and "RSTRING" allowed for attribute "contentType"*'
	'*Could not detect required attribute "myContentType" on output port 0. Or specify a valid value for "contentTypeAttributeName"*'

	'*Only types "USTRING" and "RSTRING" allowed for attribute "request"*'
	'*Could not detect required attribute "myRequest" on output port 0. Or specify a valid value for "requestAttributeName"*'

	'*Only type of "MAP" allowed for attribute "header"*'
	'*Could not detect required attribute "myHeader" on output port 0. Or specify a valid value for "headerAttributeName"*'

	'*Only types "USTRING" and "RSTRING" allowed for attribute "method"*'
	'*Could not detect required attribute "myMethod" on output port 0. Or specify a valid value for "methodAttributeName"*'

	'*Only types "USTRING" and "RSTRING" allowed for attribute "pathInfo"*'
	'*Could not detect required attribute "myPathInfo" on output port 0. Or specify a valid value for "pathInfoAttributeName"*'

	'*Only types "USTRING" and "RSTRING" allowed for attribute "contextPath"*'
	'*Could not detect required attribute "myContextPath" on output port 0. Or specify a valid value for "contextPathAttributeName"*'

	'*Only types "USTRING" and "RSTRING" allowed for attribute "url"*'
	'*Could not detect required attribute "myUrl" on output port 0. Or specify a valid value for "urlAttributeName"*'
)
