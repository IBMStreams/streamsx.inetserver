#--variantList='tuple json'
#--exclusive=true

setCategory 'quick'

PREPS=(
	'myExplain'
	'copyAndMorphSpl'
	'splCompile'
)
STEPS=(
	'submitJob -P jettyPort=8080 -P tuplesExpected=3'
	'checkJobNo'
	'waitForJobHealth'
	'mySend'
	'TT_waitForFileName=data/WindowMarker'
	'waitForFinAndCheckHealth'
	'cancelJobAndLog'
	'myEval'
	'checkLogsNoError2'
)

FINS='cancelJobAndLog'

myExplain() {
	case "$TTRO_variantCase" in
	tuple) echo "variant $TTRO_variantCase - Tuple Tests with all request attributes";;
	json) echo "variant $TTRO_variantCase - Json Tests with all request attributes";;
	*) printErrorAndExit "invalid variant $TTRO_variantCase";;
	esac
}

mySend() {
	executeLogAndSuccess curl -f -H "MyHeader: MyValGet" http://localhost:8080/RequestStream/ports/analyze/0/getpath
	executeLogAndSuccess curl -f -H "MyHeader: MyValPost1" --data-ascii "data=1234567890abcdefghijklmnopqrstuvwxyz11" http://localhost:8080/RequestStream/ports/analyze/0/postpath1
	executeLogAndSuccess curl -f -H "MyHeader: MyValPost2" --data-ascii "data=1234567890abcdefghijklmnopqrstuvwxyz2" http://localhost:8080/RequestStream/ports/analyze/0/postpath2
}

myEval() {
	if [[ "$TTRO_variantCase" == tuple ]]; then
		linewisePatternMatchInterceptAndSuccess 'data/Tuples' 'true' \
		'*key=1,method="GET",request="",contentType="",contextPath="/RequestStream/ports",pathInfo="/getpath",url="*/RequestStream/ports/analyze/0/getpath",header={*"MyHeader":"MyValGet"*' \
		'*key=2,method="POST",request="data=1234567890abcdefghijklmnopqrstuvwxyz11",contentType="application/x-www-form-urlencoded",contextPath="/RequestStream/ports",pathInfo="/postpath1",url="*/RequestStream/ports/analyze/0/postpath1",header={*"Content-Type":"application/x-www-form-urlencoded"*"Content-Length":"43"*"MyHeader":"MyValPost1"*' \
		'*key=3,method="POST",request="data=1234567890abcdefghijklmnopqrstuvwxyz2",contentType="application/x-www-form-urlencoded",contextPath="/RequestStream/ports",pathInfo="/postpath2",url="*/RequestStream/ports/analyze/0/postpath2",header={*"Content-Type":"application/x-www-form-urlencoded"*"Content-Length":"42"*"MyHeader":"MyValPost2",*'
	else
		linewisePatternMatchInterceptAndSuccess 'data/Tuples' 'true' \
		'*key=1,method="GET",request="",contentType="",contextPath="/RequestStream/ports",pathInfo="/getpath",url="*/RequestStream/ports/analyze/0/getpath"*' \
		'*key=2,method="POST",request="data=1234567890abcdefghijklmnopqrstuvwxyz11",contentType="application/x-www-form-urlencoded",contextPath="/RequestStream/ports",pathInfo="/postpath1",url="*/RequestStream/ports/analyze/0/postpath1"*' \
		'*key=3,method="POST",request="data=1234567890abcdefghijklmnopqrstuvwxyz2",contentType="application/x-www-form-urlencoded",contextPath="/RequestStream/ports",pathInfo="/postpath2",url="*/RequestStream/ports/analyze/0/postpath2"*'
	fi
}