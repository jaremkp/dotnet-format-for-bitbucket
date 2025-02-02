package ut.com.degustudios.bitbucket.content;

import com.atlassian.bitbucket.content.ArchiveRequest;
import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.io.TypeAwareOutputSupplier;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.EscalatedSecurityContext;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.Operation;
import com.degustudios.bitbucket.content.CodeService;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CodeServiceTest {
    private static final String commitId = "ref/master/1";

    @Mock
    private ContentService contentService;
    @Mock
    private SecurityService securityService;
    @Mock
    private EscalatedSecurityContext context;
    @Mock
    private Repository repository;

    private Path temporaryDirectory;

    @Before
    public void initialize() throws IOException {
        when(securityService.withPermission(eq(Permission.REPO_READ), notNull(String.class))).thenReturn(context);
        when(context.call(notNull(Operation.class))).thenAnswer(invocationOnMock -> {
            Operation<Object, IOException> call = (Operation<Object, IOException>) invocationOnMock.getArguments()[0];
            return call.perform();
        });
        temporaryDirectory = Files.createTempDirectory("tests");
    }

    @After
    public void teardown() throws IOException {
        FileUtils.deleteDirectory(new File(temporaryDirectory.toString()));
    }

    @Test
    public void cleansUpTemporaryArchive() throws IOException {
        List<Path> itemsBefore = Files.list(temporaryDirectory.getParent()).collect(Collectors.toList());

        doAnswer(invocationOnMock -> {
            TypeAwareOutputSupplier supplier = (TypeAwareOutputSupplier) invocationOnMock.getArguments()[1];
            OutputStream outputStream = supplier.getStream("archive/zip");
            try (InputStream inputStream = CodeServiceTest.class.getResourceAsStream("archives/codebase.zip")) {
                CodeServiceTest.this.copy(inputStream, outputStream);
            }
            return null;
        })
        .when(contentService)
        .streamArchive(notNull(ArchiveRequest.class), notNull(TypeAwareOutputSupplier.class));

        CodeService codeService = new CodeService(contentService, securityService);

        codeService.tryDownloadRepositoryCode(
                temporaryDirectory,
                repository,
                commitId);


        List<Path> itemsAfter = Files.list(temporaryDirectory.getParent()).collect(Collectors.toList());
        assertThat(itemsAfter.size(), is(itemsBefore.size()));
    }

    void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
    }
}
