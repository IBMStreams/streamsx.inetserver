#--variantCount=3

setCategory quick

function myExplain {
	case "$TTRO_variantCase" in
	0) echo "variant $TTRO_variantCase - Tuple Mode Input port has no default attribute key";;
	1) echo "variant $TTRO_variantCase - Tuple Mode Input port key attribute with wrong type";;
	2) echo "variant $TTRO_variantCase - Tuple Mode Input port named key attribute is not available";;

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
	'myEval'
)

myEval() {
	case "$TTRO_variantSuite" in
	de_DE)
		linewisePatternMatchInterceptAndSuccess "$TT_evaluationFile" "" "${errorCodes_de_DE[$TTRO_variantCase]}";;
	fr_FR)
		linewisePatternMatchInterceptAndSuccess "$TT_evaluationFile" "" "${errorCodes_fr_FR[$TTRO_variantCase]}";;
	it_IT)
		linewisePatternMatchInterceptAndSuccess "$TT_evaluationFile" "" "${errorCodes_it_IT[$TTRO_variantCase]}";;
	es_ES)
		linewisePatternMatchInterceptAndSuccess "$TT_evaluationFile" "" "${errorCodes_es_ES[$TTRO_variantCase]}";;
	pt_BR)
		linewisePatternMatchInterceptAndSuccess "$TT_evaluationFile" "" "${errorCodes_pt_BR[$TTRO_variantCase]}";;
	ja_JP)
		linewisePatternMatchInterceptAndSuccess "$TT_evaluationFile" "" "${errorCodes_ja_JP[$TTRO_variantCase]}";;
	zh_CN)
		linewisePatternMatchInterceptAndSuccess "$TT_evaluationFile" "" "${errorCodes_zh_CN[$TTRO_variantCase]}";;
	zh_TW)
		linewisePatternMatchInterceptAndSuccess "$TT_evaluationFile" "" "${errorCodes_zh_TW[$TTRO_variantCase]}";;
	en_US)
		linewisePatternMatchInterceptAndSuccess "$TT_evaluationFile" "" "${errorCodes[$TTRO_variantCase]}";;
	esac;
}

errorCodes=(
	"*CDIST3856E Could not detect required attribute 'key' on input port 0. Or specify a valid value for parameter 'keyAttributeName'*"
	"*CDIST3857E Only type 'INT64' is allowed for attribute 'key'*"
	"*CDIST3856E Could not detect required attribute 'myKey' on input port 0. Or specify a valid value for parameter 'keyAttributeName'*"
)

errorCodes_de_DE=(
	"*CDIST3856E Das erforderliche Attribut*"
	"*CDIST3857E Nur der Typ*"
	"*CDIST3856E Das erforderliche Attribut*"
)

errorCodes_fr_FR=(
	"*CDIST3856E Impossible de détecter*"
	"*CDIST3857E Seul le type*"
	"*CDIST3856E Impossible de détecter*"
)

errorCodes_it_IT=(
	"*CDIST3856E Impossibile rilevare l'attributo*"
	"*CDIST3857E Solo il tipo*"
	"*CDIST3856E Impossibile rilevare l'attributo*"
)

errorCodes_es_ES=(
	"*CDIST3856E No se pudo detectar el atributo necesario*"
	"*CDIST3857E Solo se permite el tipo*"
	"*CDIST3856E No se pudo detectar el atributo necesario*"
)

errorCodes_pt_BR=(
	"*CDIST3856E Não foi possível detectar o atributo necessário*"
	"*CDIST3857E Somente o tipo*"
	"*CDIST3856E Não foi possível detectar o atributo necessário*"
)

errorCodes_ja_JP=(
	"*CDIST3856E input ポート 0 に必須属性*"
	"*CDIST3857E 属性*"
	"*CDIST3856E input ポート 0 に必須属性*"
)

errorCodes_zh_CN=(
	"*CDIST3856E 无法检测 input 端口*"
	"*CDIST3857E 仅允许对属性*"
	"*CDIST3856E 无法检测 input 端口*"
)

errorCodes_zh_TW=(
	"*CDIST3856E 在 input 埠*"
	"*CDIST3857E 屬性*"
	"*CDIST3856E 在 input 埠*"
)
