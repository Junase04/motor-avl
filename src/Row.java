import java.util.HashMap;
import java.util.Map;

public class Row {
    private Map<String, Object> columnas;

    public Row() {
        this.columnas = new HashMap<>();
    }

    public void put(String nombreColumna, Object valor) {
        columnas.put(nombreColumna, valor);
    }

    public Object get(String nombreColumna) {
        return columnas.get(nombreColumna);
    }

    @Override
    public String toString() {
        return columnas.toString();
    }
}