package com.arena.ai.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages multiple Arena.ai accounts by storing/restoring cookies per account.
 * Each account stores its own set of cookies, allowing quick switching.
 */
public class AccountManager {

    private static final String PREFS_NAME = "arena_accounts";
    private static final String KEY_ACCOUNTS = "accounts";
    private static final String KEY_ACTIVE_ACCOUNT = "active_account_id";

    private final Context context;
    private final SharedPreferences prefs;

    public AccountManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get all saved accounts.
     */
    public List<Account> getAccounts() {
        List<Account> accounts = new ArrayList<>();
        String json = prefs.getString(KEY_ACCOUNTS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                accounts.add(Account.fromJson(obj));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    /**
     * Save accounts list.
     */
    private void saveAccounts(List<Account> accounts) {
        JSONArray arr = new JSONArray();
        for (Account acc : accounts) {
            try {
                arr.put(acc.toJson());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putString(KEY_ACCOUNTS, arr.toString()).apply();
    }

    /**
     * Get the currently active account ID.
     */
    public String getActiveAccountId() {
        return prefs.getString(KEY_ACTIVE_ACCOUNT, null);
    }

    /**
     * Set the active account ID.
     */
    public void setActiveAccountId(String id) {
        prefs.edit().putString(KEY_ACTIVE_ACCOUNT, id).apply();
    }

    /**
     * Get the active account object.
     */
    public Account getActiveAccount() {
        String id = getActiveAccountId();
        if (id == null) return null;
        for (Account acc : getAccounts()) {
            if (acc.id.equals(id)) return acc;
        }
        return null;
    }

    /**
     * Save current cookies as a named account.
     * @param name Display name for the account
     * @return The created account, or null if name already exists
     */
    public Account saveCurrentSession(String name) {
        String cookies = CookieManager.getInstance().getCookie("https://arena.ai");
        String id = "acc_" + System.currentTimeMillis();

        Account account = new Account(id, name, cookies, System.currentTimeMillis());

        List<Account> accounts = getAccounts();
        accounts.add(account);
        saveAccounts(accounts);
        setActiveAccountId(id);

        return account;
    }

    /**
     * Switch to a different account by loading its cookies.
     * @param id Account ID to switch to
     */
    public void switchToAccount(String id) {
        // First, save current cookies to current account
        String currentId = getActiveAccountId();
        if (currentId != null) {
            String currentCookies = CookieManager.getInstance().getCookie("https://arena.ai");
            List<Account> accounts = getAccounts();
            for (Account acc : accounts) {
                if (acc.id.equals(currentId)) {
                    acc.cookies = currentCookies;
                    break;
                }
            }
            saveAccounts(accounts);
        }

        // Clear all cookies
        CookieManager.getInstance().removeAllCookies(null);

        // Load new account's cookies
        Account target = null;
        for (Account acc : getAccounts()) {
            if (acc.id.equals(id)) {
                target = acc;
                break;
            }
        }

        if (target != null && target.cookies != null) {
            String[] cookiePairs = target.cookies.split(";");
            for (String cookie : cookiePairs) {
                String trimmed = cookie.trim();
                if (!trimmed.isEmpty()) {
                    CookieManager.getInstance().setCookie("https://arena.ai", trimmed);
                }
            }
        }

        setActiveAccountId(id);
    }

    /**
     * Delete an account.
     * @param id Account ID to delete
     */
    public void deleteAccount(String id) {
        List<Account> accounts = getAccounts();
        accounts.removeIf(acc -> acc.id.equals(id));
        saveAccounts(accounts);

        if (id.equals(getActiveAccountId())) {
            setActiveAccountId(accounts.isEmpty() ? null : accounts.get(0).id);
        }
    }

    /**
     * Rename an account.
     */
    public void renameAccount(String id, String newName) {
        List<Account> accounts = getAccounts();
        for (Account acc : accounts) {
            if (acc.id.equals(id)) {
                acc.name = newName;
                break;
            }
        }
        saveAccounts(accounts);
    }

    /**
     * Account data class.
     */
    public static class Account {
        public String id;
        public String name;
        public String cookies;
        public long createdAt;

        public Account(String id, String name, String cookies, long createdAt) {
            this.id = id;
            this.name = name;
            this.cookies = cookies;
            this.createdAt = createdAt;
        }

        public static Account fromJson(JSONObject obj) throws JSONException {
            return new Account(
                    obj.getString("id"),
                    obj.getString("name"),
                    obj.optString("cookies", null),
                    obj.optLong("createdAt", 0)
            );
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("name", name);
            obj.put("cookies", cookies);
            obj.put("createdAt", createdAt);
            return obj;
        }
    }
}
