package dev.vepo.jsonata.conformance;

import dev.vepo.jsonata.functions.PathBindings;

public final class ConformanceDiagnostics {

    public static void main(String[] args) throws Exception {
        var cases = ConformanceCase.loadAll();
        int pass = 0, fail = 0, error = 0;
        for (var c : cases) {
            if (ConformanceSkipList.isSkipped(c.group(), c.caseName())) {
                continue;
            }
            PathBindings.clearBindings();
            PathBindings.clearParents();
            try {
                if (c.run()) {
                    pass++;
                } else {
                    fail++;
                    if (fail <= 5) {
                        System.out.println("FAIL " + c.group() + "/" + c.caseName() + " " + c.expr());
                    }
                }
            } catch (StackOverflowError e) {
                error++;
                System.out.println("SOE " + c.group() + "/" + c.caseName() + " " + c.expr());
                break;
            } catch (Exception e) {
                error++;
                if (error <= 10) {
                    System.out.println("ERR " + c.group() + "/" + c.caseName() + " " + e.getClass().getSimpleName()
                            + ": " + e.getMessage());
                }
            }
        }
        System.out.printf("pass=%d fail=%d error=%d total=%d%n", pass, fail, error, cases.size());
    }
}
