package edu.neu.ccs.pyramid.experiment;

import edu.neu.ccs.pyramid.configuration.Config;
import edu.neu.ccs.pyramid.elasticsearch.ESIndex;
import edu.neu.ccs.pyramid.elasticsearch.ESIndexBuilder;
import edu.neu.ccs.pyramid.util.DirWalker;
import org.elasticsearch.index.query.MatchQueryBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * check purity of features
 * Created by chengli on 9/17/14.
 */
public class Exp6 {
    public static void main(String[] args) throws Exception{
        if (args.length !=1){
            throw new IllegalArgumentException("please specify the config file");
        }

        Config config = new Config(args[0]);
        System.out.println(config);

        ESIndex index = loadIndex(config);
        String dir = config.getString("inputFolder");
        List<File> files = DirWalker.getFiles(dir);


        System.out.println(files);
        files.sort(Comparator.comparing(file ->
                Integer.parseInt(file.getName())));

        for (File file: files){
            processOneFile(file,index,config);
        }
        System.out.println(index.getNumDocs());
        index.close();

    }

    static ESIndex loadIndex(Config config) throws Exception{

        ESIndexBuilder builder = ESIndexBuilder.builder()
                .setIndexName(config.getString("index.indexName"))
                .setClusterName(config.getString("index.clusterName"))
                .setClientType(config.getString("index.clientType"))
                .setLabelField(config.getString("index.labelField"))
                .setExtLabelField(config.getString("index.extLabelField"));
        if (config.getString("index.clientType").equals("transport")){
            String[] hosts = config.getString("index.hosts").split(Pattern.quote(","));
            String[] ports = config.getString("index.ports").split(Pattern.quote(","));
            builder.addHostsAndPorts(hosts,ports);
        }
        ESIndex index = builder.build();
        return index;
    }

    public static void processOneFile(File file, ESIndex index, Config config ) throws Exception{
        int slop = config.getInt("slop");
        int label = Integer.parseInt(file.getName());
        String extLabel;
        List<String> phrases = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String line;
            extLabel = br.readLine();
            while((line=br.readLine())!=null){
                phrases.add(line);
            }
        }
        System.out.println("==============================");
        System.out.println("class = "+extLabel);
        Set<String> terms = new HashSet<>();
        for (String phrase: phrases){
            String[] split = phrase.split(" ");
            for (String term: split){
                terms.add(term);
            }
        }


        for (String phrase:phrases){
            StringBuilder sb = new StringBuilder();
            sb.append(phrase);
            int align = 35;
            while(sb.length()<align){
                sb.append(" ");
            }
//            sb.append("phrase matches with slop 0 = ").append(index.phraseDFForClass("body",phrase,0,"label",label))
//                    .append("/").append(index.phraseDF("body",phrase,0));
//            align += 40;
//            while(sb.length()<align){
//                sb.append(" ");
//            }
            sb.append("phrase matches with slop ").append(slop).append(" = ")
                    .append(index.phraseDFForClass("body",phrase,slop,"label",label)).append("/")
                    .append(index.phraseDF("body",phrase,slop));
            align += 40;
            while(sb.length()<align){
                sb.append(" ");
            }
            sb.append("AND matches = ").append(index.DFForClass("body",phrase, MatchQueryBuilder.Operator.AND,"label",label))
                    .append("/").append(index.DF("body",phrase, MatchQueryBuilder.Operator.AND));
            System.out.println(sb.toString());
        }
//        System.out.println("---------------");
//        for (String phrase:terms){
//            StringBuilder sb = new StringBuilder();
//            sb.append(phrase);
//            int align = 35;
//            while(sb.length()<align){
//                sb.append(" ");
//            }
//            sb.append("matches = ").append(index.DFForClass("body",phrase,MatchQueryBuilder.Operator.AND,"label",label))
//                    .append("/").append(index.DF("body",phrase, MatchQueryBuilder.Operator.AND));
//            System.out.println(sb.toString());
//        }


    }
}