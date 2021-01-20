//package org.meteogroup.jbrotli.httpclient.apache;
//
//import java.io.IOException;
//import java.io.InputStream;
//
//import org.apache.http.HttpEntity;
//import org.apache.http.client.entity.DecompressingEntity;
//import org.apache.http.client.entity.InputStreamFactory;
//import org.meteogroup.jbrotli.io.BrotliInputStream;
//
//public class BrotliDecompressingEntity extends DecompressingEntity {
//  BrotliDecompressingEntity(HttpEntity entity) {
//    super(entity, new InputStreamFactory() {
//      public InputStream create(InputStream instream) throws IOException {
//        return new BrotliInputStream(instream);
//      }
//    });
//  }
//}