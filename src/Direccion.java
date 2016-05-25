public enum Direccion {
    N(1.0f), S(1.0f), E(1.0f), O(1.0f),
    NE(1.4f), NO(1.4f), SO(1.4f), SE(1.4f);
    
    private final float distancia;
    Direccion(float distancia) {
      this.distancia = distancia;
    }
    float distancia() { return distancia; }
}

