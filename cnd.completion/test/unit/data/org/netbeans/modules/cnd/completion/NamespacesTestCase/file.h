
namespace S1 {
    int i1;
    void f1();
    namespace S2 {
        int i2;
        void f2();
    }
    
    struct str {
        int i;
    } q;
}

namespace S3 {
    namespace S4 {
        class S4Class {
            public:
                void s4ClassFun();
                static void s4ClassStFun();
        };
        namespace S5 {
            class S5Class {
                public:
                    void s5ClassFun();
                    static S5Class* stS5ClassFun();
                    static S5Class* pPtrS5Class;
                    static S5Class  s5Class;
            };
        }
        S5::S5Class* S4Fun() {
        }
        S4Class* pPtrS4Class;
        S4Class  s4Class;
    }
}