package com.jmartin.temaki;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.jmartin.temaki.dialog.GenericAlertDialog;
import com.jmartin.temaki.dialog.GenericInputDialog;

import java.util.ArrayList;

/**
 * Author: Jeff Martin, 2013
 */
public class MainListsFragment extends Fragment
        implements GenericInputDialog.GenericInputDialogListener, GenericAlertDialog.GenericAlertDialogListener {

    private final String EDIT_ITEM_DIALOG_TITLE = "Edit List Item:";
    public static final int CANCEL_RESULT_CODE = 0;
    public static final int DELETE_ITEM_ID = 1;
    public static final int EDIT_ITEM_ID = 2;
    private ListView itemsListView;
    private EditText addItemsEditText;
    private ArrayAdapter<String> itemsListAdapter;

    private String listName;
    private ArrayList<String> listItems;
    private MainDrawerActivity parentActivity;

    private ActionMode actionMode;

    /* Used for keeping track of selected item. Ideally don't want to do it this way but isSelected
    * is not working in the clicklistener below.*/
    private int selectedItemPos = -1;

    public MainListsFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment, container, false);

        getActivity().setTitle(listName);

        if (savedInstanceState != null) {
            listItems = savedInstanceState.getStringArrayList(parentActivity.LIST_ITEMS_BUNDLE_KEY);
        }

        itemsListView = (ListView) view.findViewById(R.id.mainListView);
        addItemsEditText = (EditText) view.findViewById(R.id.addItemEditText);

        itemsListAdapter = new ArrayAdapter<String>(parentActivity.getApplicationContext(), R.layout.main_list_item, listItems);
        itemsListView.setAdapter(itemsListAdapter);
        itemsListView.setOnItemClickListener(new ListItemClickListener());

        addItemsEditText.setOnEditorActionListener(new NewItemsEditTextListener());

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(parentActivity.LIST_ITEMS_BUNDLE_KEY, listItems);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onFinishAlertDialog() {
        listItems.remove(selectedItemPos);
        itemsListAdapter.notifyDataSetChanged();
        clearItemSelection();
    }

    @Override
    public void onFinishDialog(String inputValue) {
        listItems.remove(selectedItemPos);
        listItems.add(selectedItemPos, inputValue);
        itemsListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == DELETE_ITEM_ID) {
            onFinishAlertDialog();
        } else if (resultCode == EDIT_ITEM_ID) {
            onFinishDialog(data.getStringExtra(GenericInputDialog.INTENT_RESULT_KEY));
        } else if (resultCode == CANCEL_RESULT_CODE) {
            clearItemSelection();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void clearItemSelection() {
        actionMode.finish();
        selectedItemPos = -1;
        itemsListAdapter.notifyDataSetChanged();
    }

    /**
     * Initialize the class' parentActivity context.
     * @param parentActivity the parent Activity to use.
     */
    public void initFragment(MainDrawerActivity parentActivity, String listName, ArrayList<String> list) {
        this.parentActivity = parentActivity;
        this.listItems = list;
        this.listName = listName;
    }

    public String getListName() {
        return listName;
    }

    public ArrayList<String> getListItems() {
        return listItems;
    }

    /**
     * Show the Edit Item prompt dialog.
     */
    private void showEditItemDialog() {
        FragmentManager fragManager = getFragmentManager();
        GenericInputDialog dialog = new GenericInputDialog(listItems.get(selectedItemPos));

        dialog.setTargetFragment(this, EDIT_ITEM_ID);
        dialog.setTitle(EDIT_ITEM_DIALOG_TITLE);
        dialog.show(fragManager, "generic_name_dialog_fragment");
    }

    /**
     * Show the Delete Item prompt dialog.
     */
    private void showDeleteItemConfirmationDialog() {
        FragmentManager fragManager = getFragmentManager();
        GenericAlertDialog dialog = new GenericAlertDialog();

        dialog.setTargetFragment(this, DELETE_ITEM_ID);
        dialog.setTitle(parentActivity.CONFIRM_DELETE_DIALOG_TITLE);
        dialog.show(fragManager, "generic_alert_dialog_fragment");
    }

    private class NewItemsEditTextListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            String newItem = v.getText().toString().trim();
            if ((actionId == EditorInfo.IME_ACTION_DONE) && (newItem.length() > 0)){
                listItems.add(newItem);
                itemsListAdapter.notifyDataSetChanged();
                v.setText("");
                return true;
            }
            return false;
        }
    }

    private class ListItemClickListener implements android.widget.AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (selectedItemPos == position) {
                view.setSelected(false);
                clearItemSelection();
            } else {
                view.setSelected(true);
                selectedItemPos = position;

                // Show Contextual ActionBar
                actionMode = parentActivity.startActionMode(actionModeCallback);
            }
        }
    }

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.main_list_item_context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.context_menu_edit:
                    showEditItemDialog();
                    return true;
                case R.id.context_menu_delete:
                    showDeleteItemConfirmationDialog();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
        }
    };
}
