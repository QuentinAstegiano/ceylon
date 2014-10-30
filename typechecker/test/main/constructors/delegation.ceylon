class Point {
    shared Float x;
    shared Float y;
    shared new Point(Float xx, Float yy) {
        x = xx;
        y = yy;
    }
    shared sealed new Diagonal(Float d) {
        x = d;
        y = d;
    }
}

class A extends Point {
    shared new New() extends Diagonal(0.0) {}
}

class B extends Point {
    shared new Pt(Float x) extends super.Point(x,0.0) {}
}

class C1 extends A {
    @error new New() extends Point(1.0,0.0) {}
}
@error class C2 extends New {
    new New() extends New() {}
}
class C3 extends A {
    @error new New() extends New() {}
}
class C4 extends A {
    new New() extends super.New() {}
}
class C5 extends A {
    @error new New() extends super.Point(1.0,2.0) {}
}
class C6 extends B {
    new New() extends super.Pt(1.0) {}
}
class C7 extends B {
    new New() extends Pt(1.0) {}
}