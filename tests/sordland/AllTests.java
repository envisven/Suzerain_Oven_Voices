package sordland;

import java.nio.file.*;


public final class AllTests {
    private AllTests() {}
    public static void main(String[] args)throws Exception {
        long start=System.nanoTime();
        JsonChecks.run();LoaderChecks.run();SemanticChecks.run();DialogueChecks.run();LayoutChecks.run();ViewportChecks.run();TextChecks.run();ActorFilterChecks.run();EdgeChecks.run();
        Path data=Path.of(args.length==0?"data":args[0]);
        var dataset=DataChecks.run(data);
        GameFlowLoaderChecks.run(data,dataset);
        RootedCampaignChecks.run(dataset);
        CampaignLayoutChecks.run(dataset);
        DialogueChecks.realData(dataset);
        System.out.printf("PASS: %,d checks in %.2f s%n",TestSupport.count(),(System.nanoTime()-start)/1_000_000_000.0);
    }
}
