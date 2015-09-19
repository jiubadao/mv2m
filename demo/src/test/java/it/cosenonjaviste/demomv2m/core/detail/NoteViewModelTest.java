package it.cosenonjaviste.demomv2m.core.detail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.concurrent.Executor;

import it.cosenonjaviste.demomv2m.R;
import it.cosenonjaviste.demomv2m.core.MessageManager;
import it.cosenonjaviste.demomv2m.core.TestExecutor;
import it.cosenonjaviste.demomv2m.model.Note;
import it.cosenonjaviste.demomv2m.model.NoteLoaderService;
import it.cosenonjaviste.demomv2m.model.NoteSaverService;
import it.cosenonjaviste.demomv2m.model.SaveResponse;
import it.cosenonjaviste.mv2m.ActivityResult;
import retrofit.RetrofitError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NoteViewModelTest {

    @Mock NoteLoaderService noteLoaderService;

    @Mock NoteSaverService noteSaverService;

    @Mock MessageManager messageManager;

    @Captor ArgumentCaptor<Note> captor;

    @Spy Executor executor = new TestExecutor();

    @InjectMocks NoteViewModel viewModel;

    @Before
    public void setUp() {
        when(noteLoaderService.load(anyString())).thenReturn(new Note("123", "title", "text"));
    }

    @Test
    public void testLoadData() {
        NoteModel model = viewModel.initAndResume(new NoteModel("123"));

        assertThat(model.getTitle().get()).isEqualTo("title");
        assertThat(model.getText().get()).isEqualTo("text");
    }

    @Test
    public void testErrorLoadingData() {
        when(noteLoaderService.load(anyString()))
                .thenThrow(RetrofitError.networkError("url", new IOException()));

        NoteModel model = viewModel.initAndResume(new NoteModel("123"));

        verify(messageManager, never()).showMessage(anyInt());
        assertThat(model.getError().get()).isTrue();
    }

    @Test
    public void testReloadAfterError() {
        when(noteLoaderService.load(anyString()))
                .thenThrow(RetrofitError.networkError("url", new IOException()))
                .thenReturn(new Note("123", "title", "text"));

        NoteModel model = viewModel.initAndResume(new NoteModel("123"));

        assertThat(model.getError().get()).isTrue();

        viewModel.reloadData();

        assertThat(model.getError().get()).isFalse();
        assertThat(model.getTitle().get()).isEqualTo("title");
    }

    @Test
    public void testValidation() {
        NoteModel model = viewModel.initAndResume(new NoteModel("123"));

        model.getTitle().set("");
        model.getText().set("");

        viewModel.save();

        assertThat(model.getTitleError().get()).isEqualTo(R.string.mandatory_field);
        assertThat(model.getTextError().get()).isEqualTo(R.string.mandatory_field);

        verify(noteSaverService, never()).save(anyString(), any(Note.class));
        verify(messageManager, never()).showMessage(anyInt());
    }

    @Test
    public void testSaveData() {
        NoteModel model = viewModel.initAndResume(new NoteModel("123"));

        model.getTitle().set("newTitle");
        model.getText().set("newText");

        viewModel.save();

        verify(noteSaverService).save(eq("123"), captor.capture());

        assertThat(captor.getValue())
                .isEqualToComparingFieldByField(new Note(null, "newTitle", "newText"));

        verify(messageManager).showMessage(eq(R.string.note_saved));
    }

    @Test
    public void testErrorSavingData() {
        when(noteSaverService.save(anyString(), any(Note.class)))
                .thenThrow(RetrofitError.networkError("url", new IOException()));

        NoteModel model = viewModel.initAndResume(new NoteModel("123"));

        model.getTitle().set("newTitle");
        model.getText().set("newText");

        viewModel.save();

        verify(messageManager).showMessage(eq(R.string.error_saving_note));
    }

    @Test
    public void testCreateNewNote() {
        when(noteSaverService.createNewNote(any(Note.class)))
                .thenReturn(new SaveResponse("newId"));

        NoteModel model = viewModel.initAndResume(new NoteModel());

        model.getTitle().set("newTitle");
        model.getText().set("newText");

        viewModel.save();

        verify(noteSaverService).createNewNote(captor.capture());

        assertThat(captor.getValue())
                .isEqualToComparingFieldByField(new Note(null, "newTitle", "newText"));
        assertThat(model.getNoteId()).isEqualTo("newId");
    }

    @Test
    public void testOnBack() {
        when(noteSaverService.createNewNote(any(Note.class)))
                .thenReturn(new SaveResponse("newId"));

        NoteModel model = viewModel.initAndResume(new NoteModel());

        model.getTitle().set("newTitle");
        model.getText().set("newText");

        viewModel.save();
        ActivityResult result = viewModel.onBackPressed();

        assertThat(result).isNotNull();
        assertThat(result.getData())
                .isEqualToComparingFieldByField(new Note("newId", "newTitle", "newText"));
        assertThat(result.isResultOk()).isTrue();
    }
}