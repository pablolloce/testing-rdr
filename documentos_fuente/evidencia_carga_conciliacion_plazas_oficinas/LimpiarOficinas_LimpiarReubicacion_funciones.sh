function LimpiarOficinas(){
    head -1 $ARG1/oficinas.csv > $ARG1/tempOfi.csv || error_exit "$LINENO" "head"
    grep "^0182;" $ARG1/oficinas.csv >> $ARG1/tempOfi.csv || error_exit "$LINENO" "grep"
    mv $ARG1/oficinas.csv $ARG1/old/oficinas_prelimpieza.csv || error_exit "$LINENO" "mv"
    mv $ARG1/tempOfi.csv $ARG1/oficinas.csv || error_exit "$LINENO" "mv"    
}
function LimpiarReubicacion(){
  cut -f 1,2,5,6 -d ";" $ARG1/Reubicacion.csv | sort -ur > $ARG1/Reubicacion.tmp || error_exit "$LINENO" "cut"
}
