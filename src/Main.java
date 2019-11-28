import bt.Bt;
import bt.data.Storage;
import bt.data.file.FileSystemStorage;
import bt.dht.DHTConfig;
import bt.dht.DHTModule;
import bt.metainfo.MetadataService;
import bt.metainfo.Torrent;
import bt.runtime.BtClient;
import com.google.inject.Module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {

        File downloadDirectory = new File("Download");
        if (!downloadDirectory.exists()) {
            if (!downloadDirectory.mkdir())
                throw new IOException("Cannot make dir " + downloadDirectory.getName());
        }
        String pathToFile = "1.torrent";
        File torrentFile = new File(pathToFile);
        if (torrentFile.exists()) {
            MetadataService metadataService = new MetadataService();
            Torrent torrent = metadataService.fromInputStream(new FileInputStream(torrentFile));
            System.out.println("URLs:");
            for (List<String> urls : torrent.getAnnounceKey().get().getTrackerUrls()) {
                for (String url : urls) {
                    System.out.println(url);
                }
            }
            System.out.println("Creation date:");
            System.out.println(torrent.getCreationDate().get());
            System.out.println("Size:");
            System.out.println(torrent.getSize());

            System.out.println(torrent.getTorrentId());

            Module dhtModule = new DHTModule(new DHTConfig() {
                @Override
                public boolean shouldUseRouterBootstrap() {
                    return true;
                }
            });

            Storage storage = new FileSystemStorage(downloadDirectory.toPath());

            BtClient client = Bt.client()
                    .storage(storage)
                    .torrent(torrentFile.toURI().toURL())
                    .autoLoadModules()
                    .module(dhtModule)
                    .stopWhenDownloaded()
                    .build();

            long startTime = System.currentTimeMillis();
            System.out.println("Download start at " + new Date(startTime));

            client.startAsync(state -> {
                if (state.getPiecesRemaining() == 0) {
                    client.stop();
                }
                }, 1000).join();

            long finishTime = System.currentTimeMillis();
            System.out.println("Download finish at " + new Date(finishTime));
            long downloadTime = finishTime - startTime;
            System.out.println("Time: " + Duration.ofMillis(downloadTime));
        } else {
            throw new FileNotFoundException("File not found! Path to file: " + pathToFile);
        }
    }
}
