public enum Direccion {
    N(1.0f, 90), S(1.0f, 270), E(1.0f, 0), O(1.0f, 180),
    NE(1.4f, 45), NO(1.4f, 135), SO(1.4f, 225), SE(1.4f, 315);
    
    private final float distancia;
    private final int angulo;
    Direccion(float distancia, int angulo) {
      this.distancia = distancia;
      this.angulo = angulo;
    }
    float distancia() { return distancia; }
    int angulo() { return angulo; }
}

