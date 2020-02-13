#--variantCount=2

setCategory quick

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
	"*CDIST3866E Port Attribute message must be of type rstring, ustring or blob*"
	"*CDIST3856E Could not detect required attribute 'myMessageAttribute' on output port 0. Or specify a valid value for parameter 'messageAttributeName'*"
)

errorCodes_de_DE=(
	"*CDIST3866E Das Portattribut message muss den Typ*"
	"*CDIST3856E Das erforderliche Attribut*"
)

errorCodes_fr_FR=(
	"*CDIST3866E L'attribut de port message doit être de type*"
	"*CDIST3856E Impossible de détecter l'attribut*"
)

errorCodes_it_IT=(
	"*CDIST3866E L'attributo della porta message deve essere di tipo*"
	"*CDIST3856E Impossibile rilevare l'attributo richiesto*"
)

errorCodes_es_ES=(
	"*CDIST3866E El atributo de puerto message debe ser de tipo*"
	"*CDIST3856E No se pudo detectar el atributo necesario*"
)

errorCodes_pt_BR=(
	"*CDIST3866E O atributo de porta message deve ser do tipo*"
	"*CDIST3856E Não foi possível detectar o atributo necessário*"
)

errorCodes_ja_JP=(
	"*CDIST3866E ポート属性*"
	"*CDIST3856E output ポート 0 に必須属性*"
)

errorCodes_zh_CN=(
	"*CDIST3866E 端口属性*"
	"*CDIST3856E 无法检测*"
)

errorCodes_zh_TW=(
	"*CDIST3866E 埠屬性*"
	"*CDIST3856E 在*"
)
