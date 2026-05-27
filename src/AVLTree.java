public class AVLTree {
    
    private Node root;

    public static class Node {
        public int key;
        public Row record;
        public Node left;
        public Node right;
        public int height;

        public Node(int key, Row record) {
            this.key = key;
            this.record = record;
            this.height = 1;
        }
    }

    public AVLTree() {
        this.root = null;
    }

    public Node getRoot() {
        return this.root;
    }

    // Método requerido para la atomicidad de la clase Tabla
    public void setRoot(Node root) {
        this.root = root;
    }

    private int height(Node n) {
        return (n == null) ? 0 : n.height;
    }

    private int getBalance(Node n) {
        return (n == null) ? 0 : height(n.right) - height(n.left);
    }

    private Node rightRotate(Node y) {
        Node x = y.left;
        Node T2 = x.right;

        x.right = y;
        y.left = T2;

        y.height = Math.max(height(y.left), height(y.right)) + 1;
        x.height = Math.max(height(x.left), height(x.right)) + 1;

        return x;
    }

    private Node leftRotate(Node x) {
        Node y = x.right;
        Node T2 = y.left;

        y.left = x;
        x.right = T2;

        x.height = Math.max(height(x.left), height(x.right)) + 1;
        y.height = Math.max(height(y.left), height(y.right)) + 1;

        return y;
    }

    public void insertar(int key, Row record) {
        this.root = insertRec(this.root, key, record);
    }

    private Node insertRec(Node node, int key, Row record) {
        if (node == null) {
            return new Node(key, record);
        }

        if (key < node.key) {
            node.left = insertRec(node.left, key, record);
        } else if (key > node.key) {
            node.right = insertRec(node.right, key, record);
        } else {
            return node;
        }

        node.height = 1 + Math.max(height(node.left), height(node.right));
        int balance = getBalance(node);

        if (balance < -1 && key < node.left.key) {
            return rightRotate(node);
        }

        if (balance > 1 && key > node.right.key) {
            return leftRotate(node);
        }

        if (balance < -1 && key > node.left.key) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }

        if (balance > 1 && key < node.right.key) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        return node;
    }

    public Row buscar(int key) {
        return searchRec(this.root, key);
    }

    private Row searchRec(Node node, int key) {
        if (node == null) return null;
        if (node.key == key) return node.record;

        return (key < node.key) ? searchRec(node.left, key) : searchRec(node.right, key);
    }

    public void eliminar(int key) {
        this.root = deleteRec(this.root, key);
    }

    private Node deleteRec(Node root, int key) {
        if (root == null) {
            return root;
        }

        if (key < root.key) {
            root.left = deleteRec(root.left, key);
        } else if (key > root.key) {
            root.right = deleteRec(root.right, key);
        } else {
            if ((root.left == null) || (root.right == null)) {
                Node temp = (root.left != null) ? root.left : root.right;

                if (temp == null) {
                    temp = root;
                    root = null;
                } else {
                    root = temp; 
                }
            } else {
                Node temp = minValueNode(root.right);
                root.key = temp.key;
                root.record = temp.record;
                root.right = deleteRec(root.right, temp.key);
            }
        }

        if (root == null) {
            return root;
        }

        root.height = Math.max(height(root.left), height(root.right)) + 1;
        int balance = getBalance(root);

        if (balance < -1) {
            if (getBalance(root.left) <= 0) {
                return rightRotate(root);
            } else {
                root.left = leftRotate(root.left);
                return rightRotate(root);
            }
        }

        if (balance > 1) {
            if (getBalance(root.right) >= 0) {
                return leftRotate(root);
            } else {
                root.right = rightRotate(root.right);
                return leftRotate(root);
            }
        }

        return root;
    }

    private Node minValueNode(Node node) {
        Node current = node;
        while (current.left != null) {
            current = current.left;
        }
        return current;
    }

    public void inorder() {
        inorderRec(this.root);
    }

    private void inorderRec(Node node) {
        if (node != null) {
            inorderRec(node.left);
            System.out.println("  ID: " + node.key + " -> " + node.record.toString());
            inorderRec(node.right);
        }
    }
}