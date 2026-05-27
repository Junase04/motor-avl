public class Node {
    public int key;         // identificador
    public Row record;      // Datos de la fila
    public Node left;       // Hijo izquierdo
    public Node right;      // Hijo derecho
    public int height;      // Altura para el balanceo AVL

    public Node(int key, Row record) {
        this.key = key;
        this.record = record;
        this.height = 1;
    }
}