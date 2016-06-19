package com.boilerplate;

import com.boilerplate.rx.Whatev;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.*;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.sun.org.apache.bcel.internal.util.ClassLoader;
import javaslang.control.Try;
import jdk.management.resource.internal.CompletionHandlerWrapper;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.util.FileUtils;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

class Literals {

    public static <T> List<T> List(T... elems) {
        return Arrays.asList(elems);
    }

    public static Map<String, String> Map(String s) {
        return Splitter.on(",").omitEmptyStrings().trimResults().withKeyValueSeparator("->").split(s);
    }

    public static Map<String, String> MapN(String s) {
        return Splitter.on("\n").omitEmptyStrings().trimResults().withKeyValueSeparator("->").split(s);
    }

    public static <T1, T2> Map<T1, T2> Map(Pair<T1, T2>... p) {
        HashMap<T1, T2> hashMap = new HashMap<>();
        for (Pair<T1, T2> t1T2Pair : p) {
            hashMap.put(t1T2Pair.getLeft(), t1T2Pair.getRight());
        }
        return hashMap;
    }

    public static <T1, T2> Pair<T1, T2> P(T1 t, T2 t2) {
        return Pair.of(t, t2);
    }

    public static <T> Map<T, T> Map(T... t) {
        HashMap<T, T> outMap = new HashMap<>();
        Preconditions.checkArgument(t.length % 2 == 0, "Must be even");
        T t1 = null;
        for (int i = 0; t.length > i; i++) {
            if (i % 2 == 0)
                t1 = t[i];
            else
                outMap.put(t1, t[i]);
        }
        return outMap;
    }
}

final class IOStreams {
    public static InputStreamWrapper fluent(InputStream stream) {
        return new InputStreamWrapper(stream);
    }

    public static OutputStreamWrapper fluent(OutputStream stream) {
        Literals.Map(
                1, 2,
                3, 4,
                5, 6,
                7, 8,
                9, 10
        );
        Literals.Map(
                "siemanko->asdokzxcok",
                "whatever->asokczoxkaosd"
        );
        return new OutputStreamWrapper(stream);
    }

    public interface Consumer {
        static Consumer of(OutputStream os) {
            return () -> os;
        }

        static Consumer of(OutputStreamWrapper os) {
            return os::underlying;
        }

        OutputStream to();
    }

    public interface Supplier {
        static Supplier of(InputStream is) {
            return () -> is;
        }

        static Supplier of(InputStreamWrapper is) {
            return is::underlying;
        }

        InputStream from();
    }

    private interface IOStreamFluent {
        IOStreamFluent buffer() throws IOException;

        IOStreamFluent base64encode() throws IOException;

        IOStreamFluent base64decode() throws IOException;

        IOStreamFluent decrypt(String cipher, Key key) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException;

        IOStreamFluent encrypt(String cipher, Key key) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException;

        IOStreamFluent hash(HashFunction hashFunction) throws IOException;
    }

    private interface InputStreamFluent extends IOStreamFluent {
        void to(Consumer os) throws IOException;

        IOStreamFluent unzip() throws IOException;
    }

    private interface OutputStreamFluent extends IOStreamFluent {
        void from(Supplier os) throws IOException;

        IOStreamFluent zip() throws IOException;
    }

    public static class WhereToOutputStream {
        private InputStream is;

        private WhereToOutputStream(InputStream is) {
            this.is = is;
        }

        WhereToOutputStream of(InputStream is) {
            return new WhereToOutputStream(is);
        }

        public RunnableOutputStreamWrapper custom(OutputStream os) {
            return RunnableOutputStreamWrapper.of(is, new OutputStreamWrapper(os));
        }

        public RunnableOutputStreamWrapperStringAware stringRunnable() {
            return new RunnableOutputStreamWrapperStringAware(is, new OutputStreamWrapper(new ByteArrayOutputStream()));
        }

        // blocks
        public String string() throws IOException {
            return stringRunnable().runToString();
        }

        public Single<String> stringAsync() throws IOException {
            return stringRunnable().runAsyncToString();
        }

        // public RunnableOutputStreamWrapper net(OutputStream os) {
        //     return RunnableOutputStreamWrapper.of(is, os);
        // }
        public RunnableOutputStreamWrapper file(Path f, OpenOption... oo) throws IOException {
            return RunnableOutputStreamWrapper.of(is, OutputStreamWrapper.wrap(Files.newOutputStream(f, oo)));
        }

        public RunnableOutputStreamWrapper stdout() {
            return RunnableOutputStreamWrapper.of(is, OutputStreamWrapper.wrap(System.out));
        }
    }

    public static class RunnableOutputStreamWrapper implements OutputStreamFluent {

        protected InputStream is;
        protected OutputStreamWrapper delegate;

        RunnableOutputStreamWrapper(InputStream is, OutputStreamWrapper osw) {
            this.delegate = osw;
            this.is = is;
        }

        @Override
        public void from(Supplier sup) throws IOException {
            delegate.from(sup);
        }

        @Override
        public RunnableOutputStreamWrapper hash(HashFunction hashFunction) throws IOException {
            return new RunnableOutputStreamWrapper(is, delegate.hash(hashFunction));
        }

        @Override
        public RunnableOutputStreamWrapper encrypt(String cipher, Key key) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
            return new RunnableOutputStreamWrapper(is, delegate.encrypt(cipher, key));
        }

        @Override
        public RunnableOutputStreamWrapper zip() throws IOException {
            return new RunnableOutputStreamWrapper(is, delegate.zip());
        }

        @Override
        public RunnableOutputStreamWrapper buffer() throws IOException {
            return new RunnableOutputStreamWrapper(is, delegate.buffer());
        }

        @Override
        public RunnableOutputStreamWrapper base64encode() throws IOException {
            return new RunnableOutputStreamWrapper(is, delegate.base64encode());
        }

        @Override
        public RunnableOutputStreamWrapper base64decode() throws IOException {
            return new RunnableOutputStreamWrapper(is, delegate.base64decode());
        }

        @Override
        public RunnableOutputStreamWrapper decrypt(String cipher, Key key) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
            return new RunnableOutputStreamWrapper(is, delegate.decrypt(cipher, key));
        }

        static RunnableOutputStreamWrapper of(InputStream is, OutputStreamWrapper osw) {
            RunnableOutputStreamWrapper roswr = new RunnableOutputStreamWrapper(is, osw);
            return roswr;
        }
        // public void runAndClose() throws IOException {
        //     IOUtils.copyAndClose(is, this.underlying());
        // }

        public void run() throws IOException {
            IOUtils.copy(is, this.delegate.underlying());
        }

        public Single<Void> runAsync() {
            return runAsync(ForkJoinPool.commonPool());
        }

        public Single<Void> runAsync(Executor e) {
            return Single.<Void>fromCallable(() -> {
                run();
                return null;
            }).observeOn(Schedulers.from(e));
        }

        // public Single<Void> runAsyncAndClose() {
        //     return runAsyncAndClose(ForkJoinPool.commonPool());
        // }

        // public Single<Void> runAsyncAndClose(Executor e) {
        //     return Single.fromCallable(() -> {
        //         runAndClose(); return null;
        //     });
        // }
    }

    public static class RunnableOutputStreamWrapperStringAware extends RunnableOutputStreamWrapper {

        RunnableOutputStreamWrapperStringAware(InputStream is, OutputStreamWrapper osw) {
            super(is, osw);
        }

        public Single<String> runAsyncToString() {
            return Single.fromCallable(this::runToString).observeOn(Schedulers.from(ForkJoinPool.commonPool()));
        }

        public Single<String> runAsyncToString(Executor e) {
            return Single.fromCallable(this::runToString).observeOn(Schedulers.from(e));
        }

        public String runToString() throws IOException {
            int copied = IOUtils.copy(is, delegate.underlying());
            return delegate.underlying().toString();
        }
    }

    public static class InputStreamWrapper extends InputStream implements InputStreamFluent {
        private static InputStreamWrapper wrap(InputStream s) {
            return new InputStreamWrapper(s);
        }

        private final InputStream is;

        public InputStream underlying() {
            return is;
        }

        public <T extends InputStream> T underlyingTyped() {
            return (T) is;
        }

        InputStreamWrapper(InputStream is) {
            this.is = is;
        }

        @Override
        public int read() throws IOException {
            return is.read();
        }

        @Override
        public InputStreamWrapper unzip() throws IOException {
            return wrap(new ZipInputStream(is));
        }

        @Override
        public InputStreamWrapper buffer() throws IOException {
            return wrap(new BufferedInputStream(is));
        }

        @Override
        public InputStreamWrapper base64encode() throws IOException {
            return wrap(new Base64InputStream(is, true));
        }

        @Override
        public InputStreamWrapper base64decode() throws IOException {
            return wrap(new Base64InputStream(is, false));
        }

        @Override
        public InputStreamWrapper decrypt(String cipher, Key key) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
            Cipher ciph = Cipher.getInstance(cipher);
            ciph.init(Cipher.DECRYPT_MODE, key);
            return wrap(new CipherInputStream(is, ciph));
        }

        @Override
        public InputStreamWrapper encrypt(String cipher, Key key) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
            Cipher ciph = Cipher.getInstance(cipher);
            ciph.init(Cipher.ENCRYPT_MODE, key);
            return wrap(new CipherInputStream(is, ciph));
        }

        @Override
        public InputStreamWrapper hash(HashFunction hashFunction) throws IOException {
            return wrap(new HashingInputStream(hashFunction, is));
        }


        /**
         * blocks and copies whole content to consumer
         *
         * @param os
         * @return
         */
        @Override
        public void to(Consumer os) throws IOException {
            org.apache.commons.io.IOUtils.copy(is, os.to());
        }

        public RunnableOutputStreamWrapper to(OutputStream os) {
            return RunnableOutputStreamWrapper.of(this.underlying(), new OutputStreamWrapper(os));
        }

        public WhereToOutputStream to() {
            return new WhereToOutputStream(is);
        }
    }

    public static class OutputStreamWrapper extends OutputStream implements OutputStreamFluent {
        protected final OutputStream os;

        private static OutputStreamWrapper wrap(OutputStream s) {
            return new OutputStreamWrapper(s);
        }


        public OutputStream underlying() {
            return os;
        }

        public <T extends OutputStream> T underlyingTyped() {
            return (T) os;
        }

        OutputStreamWrapper(OutputStream os) {
            this.os = os;
        }

        @Override
        public OutputStreamWrapper zip() throws IOException {
            return wrap(new ZipOutputStream(os));
        }

        @Override
        public OutputStreamWrapper buffer() throws IOException {
            return wrap(new BufferedOutputStream(os));
        }

        @Override
        public OutputStreamWrapper base64encode() throws IOException {
            return wrap(new Base64OutputStream(os, true));
        }

        @Override
        public OutputStreamWrapper base64decode() throws IOException {
            return wrap(new Base64OutputStream(os, false));
        }

        @Override
        public OutputStreamWrapper decrypt(String cipher, Key key) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
            Cipher ciph = Cipher.getInstance(cipher);
            ciph.init(Cipher.DECRYPT_MODE, key);
            return wrap(new CipherOutputStream(os, ciph));
        }

        @Override
        public OutputStreamWrapper encrypt(String cipher, Key key) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
            Cipher ciph = Cipher.getInstance(cipher);
            ciph.init(Cipher.ENCRYPT_MODE, key);
            return wrap(new CipherOutputStream(os, ciph));
        }

        @Override
        public OutputStreamWrapper hash(HashFunction hashFunction) throws IOException {
            return wrap(new HashingOutputStream(hashFunction, os));
        }

        @Override
        public void write(int b) throws IOException {
            os.write(b);
        }

        @Override
        public void from(Supplier sup) throws IOException {
            org.apache.commons.io.IOUtils.copy(sup.from(), os);
        }
    }

}


class Whatever implements Whatev {
    public Whatever() {
    }

    private static Whatev instance1 = null;

    static {
        try {
            Class<Whatev> w = (Class<Whatev>) ClassLoader.getSystemClassLoader().loadClass("com.boilerplate.rx.WhatevImpl");

            instance1 = w.newInstance();

        } catch (ClassNotFoundException e) {

        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void functionality() {
        if (instance1 == null)
            throw new NotImplementedException("Not implemented, you should provide with maven");
        instance1.functionality();
    }
}

class Whatever1 {
    public Whatever1() {
    }

    List<Integer> getList() {
        return null;
    }
}

class Whatever2 {
    private Whatever1 w;

    public Whatever2() {
        w = new Whatever1();
    }

    public Whatever1 getW() {
        return w;
    }
}

class Asd {
    public void whatev(int a1, int a2, int a3, int a4) {

    }
}

class FileUtils2 {

    private static ByteBuffer b = ByteBuffer.allocate(4096);

    static class MutableAttachment {
        int write;
        int read;

        public MutableAttachment() {
            write = 0;
            read = 0;
        }

        public int getWrite() {
            return write;
        }

        public int getRead() {
            return read;
        }

        public void setWrite(int write) {
            this.write = write;
        }

        public void setRead(int read) {
            this.read = read;
        }
    }

    // public static Future<Path> asyncAtomicFileCopy(File f1, File f2) throws IOException {
    //     AsynchronousFileChannel readChan = AsynchronousFileChannel.open(f1.toPath(), StandardOpenOption.READ);
    //     AsynchronousFileChannel writeChan = AsynchronousFileChannel.open(f2.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    //     Files.copy
    //     CompletableFuture<FileLock> lock1 = new CompletableFuture<>();
    //     CompletableFuture<FileLock> lock2 = new CompletableFuture<>();
    //     CompletionHandler<FileLock, CompletableFuture<FileLock>> lockHandler = new CompletionHandler<FileLock, CompletableFuture<FileLock>>() {
    //         @Override
    //         public void completed(FileLock result, CompletableFuture<FileLock> attachment) {
    //             attachment.complete(result);
    //         }
    //
    //         @Override
    //         public void failed(Throwable exc, CompletableFuture<FileLock> attachment) {
    //             attachment.completeExceptionally(exc);
    //         }
    //     };
    //
    //     MutableAttachment att = new MutableAttachment();
    //
    //     CompletableFuture<Path> last = new CompletableFuture<>();
    //
    //     CompletionHandler<Integer, MutableAttachment> readHandler = null;
    //     Function<CompletionHandler<Integer, MutableAttachment>, CompletionHandler<Integer, MutableAttachment>> writeHandlerSupplier  = (CompletionHandler<Integer, MutableAttachment> readHa) ->
    //             new CompletionHandler<Integer, MutableAttachment>() {
    //                 @Override
    //                 public void completed(Integer result, MutableAttachment attachment) {
    //
    //                 }
    //
    //                 @Override
    //                 public void failed(Throwable exc, MutableAttachment attachment) {
    //
    //                 }
    //             };
    //
    //     readHandler = new CompletionHandler<Integer, MutableAttachment>() {
    //         @Override
    //         public void completed(Integer result, MutableAttachment attachment) {
    //             if (result < 0)
    //                 last.complete(f2.toPath());
    //             else {
    //                 attachment.setRead(attachment.getRead() + result);
    //                 writeChan.read(b, attachment.getRead(), attachment, writeHandler);
    //             }
    //         }
    //
    //         @Override
    //         public void failed(Throwable exc, MutableAttachment attachment) {
    //
    //         }
    //
    //     };
    //
    //
    //     readChan.lock(lock1, lockHandler);
    //     writeChan.lock(lock2, lockHandler);
    //
    //     CompletableFuture.allOf(lock1, lock2)
    //             // we got locks
    //             .thenApply(_void -> {
    //                 readChan.read(b, 0, null, readHandler);
    //             });
    //
    //     return last;
    // }
}

public class Main {
    public static Observable<String> getHelloWorldSource() {
        Stream<String> helloWorldStream = Stream.generate(() -> "Hello World");
        return Observable.from(helloWorldStream::iterator);
    }

    public static String whatever( ) throws IOException {
        return "";
    }
    public static void main(String[] args) throws Exception {

        Whatever2 whatever2 = new Whatever2();
        whatever2.getW().getList();
        FileChannel in = new FileInputStream("/Users/mihau/build.properties").getChannel();

        Whatever w = new Whatever();
        w.functionality();
        SecurityManager securityManager = new SecurityManager();
        System.setSecurityManager(securityManager);
        System.setProperty("java.security.policy", "file://Users/mihau/workspace/java-functional-boilerplate/java.security.policy");

        System.out.println(System.getSecurityManager());
        getHelloWorldSource().first().subscribe(System.out::println);
        MyAbstractExecutionThreadService service = new MyAbstractExecutionThreadService();

        // IOStreams
        //         .fluent(Files.newInputStream())
        //         .base64encode()
        //         .to()
        //         .file(Paths.get("/Users/mihau/build.properties.zip"), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
        //         .runAsync()
        //         .subscribe((res) -> {
        //             System.out.println("didit");
        //         })
        // ;
        System.out.println("I was here early :)");
        EventBus eb = new EventBus("test");
        EventListener el2 = new EventListener();
        EventListener el = new EventListener();
        eb.register(el2);
        eb.register(el);
        eb.post(10L);
        eb.unregister(el);
        eb.post(20L);
        eb.post(25L);
        eb.unregister(el2);
        eb.post(40L);
    }

    public static class EventListener {
        @Subscribe
        public void listen(Long event) {
            System.out.println("event");
            System.out.println(event);
        }
    }

    private static class MyAbstractExecutionThreadService extends AbstractExecutionThreadService {
        private int wait = 1000;

        @Override
        protected void run() throws Exception {
            while (true) {
                Thread.sleep(wait += 100);
                System.out.println("Whatever: " + wait);
                if (wait > 3000) {
                    throw new IllegalStateException("FAILED THIS BULLSHITE BWOY");
                }
            }
        }
    }
}