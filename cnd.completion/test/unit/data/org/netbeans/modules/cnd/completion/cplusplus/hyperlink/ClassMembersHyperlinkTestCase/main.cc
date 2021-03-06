
#include "ClassA.h" // in test

void go();
void go(int a);
void go(int a, double b);

void go() {
    
}

void go(int a) {
    
}

void go(int a, double b) {
    friendFoo();    
}

int main(int argc, char** argv) {
    ClassA a;
    int in = argc;
    void* ptr = argv;
    go();
    go(1);
    go(in, 1.0);

    // Prints welcome message...
    cout << "Welcome ...\n";
    
    // Prints arguments...
    if (argc > 1) {
        cout << "\nArguments:\n";
        for (int i = 1; i < argc; i++) { 
            cout << i << ": " << argv[i] << "\n";
        }
    }
    // hello;

    return 0;
}
 
void castChecks() {
    void* a;
    ((ClassB)*a).myPtr;
    ((ClassB*)a)->myPtr;
    ((ClassB)*a).myVal;
    ((ClassB*)a)->myVal;
}

void sameValue(int sameValue) {
    if (sameValue > 0) {
        sameValue(sameValue - 1);
    }
}

typedef unsigned int uint32_t;
typedef	struct ehci_itd {
    uint32_t itd_state;
} ehci_itd_t;

typedef struct ehci_state {
    ehci_itd_t *ehci_itd_pool_addr;
} ehci_state_t;

void iz136894(ehci_state* state, int i){
    state->ehci_itd_pool_addr->itd_state;
    state->ehci_itd_pool_addr[i].itd_state;
    ehci_itd_t *pool_addr;
    pool_addr[i].itd_state;
    state->ehci_itd_pool_addr[0].itd_state;
    pool_addr[0].itd_state;
}

void iz137483(int param_postfix, int param){
    int i = param;
    int j = param_postfix;
    ehci_state* state;
    state->ehci_itd_pool_addr[sizeof(param)/sizeof(char) - 1].itd_state;
}

struct entryplus3_info {
    int attr;
    int fh;
    int res;
};
typedef struct entryplus3_info entryplus3_info;

int* iz145828(entryplus3_info *infop, int i)
{
    int* j = &infop[i].attr; //
    &infop[i].fh; //
    return &infop[i].res; // 
}