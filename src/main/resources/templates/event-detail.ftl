<#include "_header.ftl">
<section>
  <div class="page-head">
    <h1>第${event.number}回 ${event.name}</h1>
    <span class="badge <#if event.isRecruiting>badge-recruit<#else>badge-archive</#if>">
      <#if event.isRecruiting>募集中<#else>アーカイブ済</#if>
    </span>
  </div>

  <dl class="detail-list">
    <dt>開催エリア</dt><dd>${event.area}</dd>
    <dt>開催日</dt><dd>${event.date}</dd>
    <dt>幹事</dt><dd>${event.organizerName}</dd>
    <dt>参加人数</dt><dd>${event.participantCount!""}</dd>
  </dl>

  <div class="actions">
    <#if event.isRecruiting>
      <#if isParticipant>
        <#if !isOrganizer>
          <form method="post" action="/g/${guildId?c}/events/${event.id?c}/leave" class="inline-form">
            <button type="submit" class="button danger">参加をキャンセル</button>
          </form>
        </#if>
      <#else>
        <form method="post" action="/g/${guildId?c}/events/${event.id?c}/join" class="inline-form">
          <button type="submit" class="button">参加する</button>
        </form>
      </#if>
    </#if>

    <#if canManage>
      <a class="button secondary" href="/g/${guildId?c}/events/${event.id?c}/edit">編集</a>
      <#if event.isRecruiting>
        <form method="post" action="/g/${guildId?c}/events/${event.id?c}/archive" class="inline-form">
          <button type="submit" class="button secondary">アーカイブ化</button>
        </form>
      <#else>
        <form method="post" action="/g/${guildId?c}/events/${event.id?c}/reopen" class="inline-form">
          <button type="submit" class="button secondary">アーカイブ解除</button>
        </form>
      </#if>
    </#if>

    <#if isAdmin>
      <a class="button danger" href="/g/${guildId?c}/events/${event.id?c}/delete">削除</a>
      <form method="post" action="/g/${guildId?c}/events/${event.id?c}/renumber" class="inline-form renumber-form">
        <input type="number" name="number" min="1" placeholder="新しい第n回" required>
        <button type="submit" class="button secondary">採番修正</button>
      </form>
    </#if>
  </div>

  <#if canManage>
    <details class="edit-details" id="transfer-organizer-details">
      <summary>幹事を変更</summary>
      <form method="post" action="/g/${guildId?c}/events/${event.id?c}/transfer-organizer" class="inline-fields">
        <select name="userId" id="organizer-candidates-select" required>
          <option value="" disabled selected>読み込み中...</option>
        </select>
        <button type="submit" class="button secondary">変更</button>
      </form>
    </details>
    <script>
      (function () {
        var select = document.getElementById("organizer-candidates-select");
        fetch("/g/${guildId?c}/events/${event.id?c}/organizer-candidates")
          .then(function (res) {
            if (!res.ok) throw new Error("failed");
            return res.text();
          })
          .then(function (html) {
            select.innerHTML = html;
          })
          .catch(function () {
            select.innerHTML = '<option value="" disabled selected>取得に失敗しました</option>';
          });
      })();
    </script>
  </#if>

  <#if (participants?? && participants?size gt 0) || guests?size gt 0>
    <h2>参加者一覧</h2>
    <ul class="participant-list">
      <#list participants as p>
        <li>${p.name}</li>
      </#list>
      <#list guests as g>
        <li>${g.displayName}</li>
      </#list>
    </ul>
  </#if>

  <#if canManage>
    <details class="edit-details" id="edit-guest-details">
      <summary>ゲストを編集</summary>
      <p class="field-hint">Discord を使っていない参加者をゲストとして登録できます。削除しても出欠・受領記録は残り、同じ名前で再追加すると元に戻ります。</p>
      <form method="post" action="/g/${guildId?c}/events/${event.id?c}/guests" class="inline-fields">
        <input type="text" name="name" placeholder="ゲスト名" required maxlength="80">
        <button type="submit" class="button secondary">追加</button>
      </form>
      <#list guests as g>
        <div class="inline-fields">
          <form method="post" action="/g/${guildId?c}/events/${event.id?c}/guests/${g.id?c}/rename" class="inline-fields">
            <input type="text" name="name" value="${g.name}" required maxlength="80">
            <button type="submit" class="button secondary">名前を変更</button>
          </form>
          <form method="post" action="/g/${guildId?c}/events/${event.id?c}/guests/${g.id?c}/delete" class="inline-form" onsubmit="return confirm('ゲスト「${g.name?js_string}」を削除しますか?');">
            <button type="submit" class="button danger small">削除</button>
          </form>
        </div>
      </#list>
    </details>
  </#if>

  <#if isAdmin>
    <details class="edit-details" id="edit-participant-details">
      <summary>参加者を編集</summary>
      <div class="inline-fields">
        <form method="post" action="/g/${guildId?c}/events/${event.id?c}/add-user" class="inline-fields">
          <select name="userId" id="addable-members-select" required>
            <option value="" disabled selected>読み込み中...</option>
          </select>
          <button type="submit" class="button secondary">追加</button>
        </form>
        <form method="post" action="/g/${guildId?c}/events/${event.id?c}/remove-user" class="inline-fields">
          <select name="userId" required>
            <option value="" disabled selected>削除するメンバーを選択</option>
            <#list participants as p>
              <#if !p.isOrganizer>
                <option value="${p.id?c}">${p.name}</option>
              </#if>
            </#list>
          </select>
          <button type="submit" class="button danger">削除</button>
        </form>
      </div>
    </details>
    <script>
      (function () {
        var select = document.getElementById("addable-members-select");
        fetch("/g/${guildId?c}/events/${event.id?c}/addable-members")
          .then(function (res) {
            if (!res.ok) throw new Error("failed");
            return res.text();
          })
          .then(function (html) {
            select.innerHTML = html;
          })
          .catch(function () {
            select.innerHTML = '<option value="" disabled selected>取得に失敗しました</option>';
          });
      })();
    </script>
  </#if>

  <h2>飲み会一覧</h2>
  <#if parties?size == 0>
    <p class="empty">まだ飲み会が登録されていません。</p>
  <#else>
    <#list parties as party>
      <div class="party-card" id="party-${party.id?c}">
        <h3>${party.orderNo}軒目: ${party.shopName}</h3>
        <p>
          会計金額: ${party.totalAmountText}
          <#if party.receivedTotalText??> / 受領合計: ${party.receivedTotalText}</#if>
          <#if party.diffText??>
            / 差額: <span class="amount-diff <#if party.diffPositive>positive<#else>negative</#if>">${party.diffText}</span>
          </#if>
        </p>
        <#if party.note??>
          <p class="note">${party.note}</p>
        </#if>

        <#if party.attendance?size gt 0>
          <#if canManage>
            <form method="post" action="/g/${guildId?c}/parties/${party.id?c}/attendance-payments">
              <p class="field-hint">出欠・受領金額を変更して、最後に更新してください。</p>
              <table class="attendance-table">
                <thead>
                  <tr>
                    <th>名前</th>
                    <th>出欠変更ボタン</th>
                    <th>受領金額</th>
                  </tr>
                </thead>
                <tbody>
                  <#list party.attendance as a>
                    <tr>
                      <td data-label="名前" class="primary-cell">${a.name}</td>
                      <td data-label="出欠変更ボタン">
                        <div class="status-toggle-group">
                          <label class="status-toggle status-toggle-present">
                            <input type="radio" name="status_${a.userId?c}" value="PRESENT"<#if a.status?? && a.status == "PRESENT"> checked</#if>>
                            <span>出席</span>
                          </label>
                          <label class="status-toggle status-toggle-absent">
                            <input type="radio" name="status_${a.userId?c}" value="ABSENT"<#if a.status?? && a.status == "ABSENT"> checked</#if>>
                            <span>欠席</span>
                          </label>
                          <label class="status-toggle status-toggle-clear">
                            <input type="radio" name="status_${a.userId?c}" value="CLEAR"<#if !a.status??> checked</#if>>
                            <span>未確認</span>
                          </label>
                        </div>
                      </td>
                      <td data-label="受領金額">
                        <label class="amount-field">
                          <span class="amount-unit">¥</span>
                          <input type="number" name="amount_${a.userId?c}" min="0" step="100" value="<#if a.receivedAmount??>${a.receivedAmount?c}</#if>" placeholder="0" inputmode="numeric">
                        </label>
                      </td>
                    </tr>
                  </#list>
                </tbody>
              </table>
              <div class="attendance-form-actions">
                <button type="button" class="link-button" onclick="this.closest('form').querySelectorAll('input[type=radio][value=PRESENT]').forEach(function(r){ r.checked = true; })">全員を出席にする</button>
                <button type="submit" class="button">出欠・受領金額を更新</button>
              </div>
            </form>
          <#else>
            <table class="attendance-table">
              <thead><tr><th>名前</th><th>出欠ステータス</th><th>受領金額</th></tr></thead>
              <tbody>
                <#list party.attendance as a>
                  <tr>
                    <td data-label="名前" class="primary-cell">${a.name}</td>
                    <td data-label="出欠ステータス" class="status-cell">
                      <#if !a.status??>❔ 未確認
                      <#elseif a.status == "PRESENT">✅ 出席
                      <#elseif a.status == "ABSENT">❌ 欠席
                      <#else>❔ 未確認
                      </#if>
                    </td>
                    <td data-label="受領金額">${a.receivedAmountText}</td>
                  </tr>
                </#list>
              </tbody>
            </table>
          </#if>
        </#if>

        <#if canManage>
          <details class="edit-details">
            <summary>お店情報を編集</summary>
            <form method="post" action="/g/${guildId?c}/parties/${party.id?c}/edit" class="inline-fields">
              <input type="text" name="shopName" value="${party.shopName}" required maxlength="120">
              <input type="number" name="totalAmount" min="0" value="${party.totalAmount!""}" placeholder="会計金額">
              <button type="submit" class="button secondary">更新</button>
            </form>
            <form method="post" action="/g/${guildId?c}/parties/${party.id?c}/delete" class="inline-form" onsubmit="return confirm('このお店を削除しますか?出欠・受領記録も削除されます。');">
              <button type="submit" class="button danger small">このお店を削除</button>
            </form>
          </details>
        </#if>
      </div>
    </#list>
  </#if>

  <#if canManage>
    <h2>飲み会(お店)を追加</h2>
    <form method="post" action="/g/${guildId?c}/events/${event.id?c}/parties" class="inline-fields">
      <input type="text" name="shopName" placeholder="店名" required maxlength="120">
      <input type="number" name="orderNo" min="1" placeholder="軒目(任意)">
      <input type="number" name="totalAmount" min="0" placeholder="会計金額(任意)">
      <button type="submit" class="button">追加</button>
    </form>
  </#if>
</section>
<#include "_footer.ftl">
