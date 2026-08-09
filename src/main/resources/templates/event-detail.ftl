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

  <#if participants?? && participants?size gt 0>
    <h2>参加者一覧</h2>
    <ul class="participant-list">
      <#list participants as p>
        <li>
          ${p.name}
          <#if isAdmin && !p.isOrganizer>
            <form method="post" action="/g/${guildId?c}/events/${event.id?c}/remove-user" class="inline-form">
              <input type="hidden" name="userId" value="${p.id?c}">
              <button type="submit" class="button danger small">削除</button>
            </form>
          </#if>
        </li>
      </#list>
    </ul>
  </#if>

  <#if isAdmin>
    <details class="edit-details" id="add-participant-details">
      <summary>参加者を追加</summary>
      <form method="post" action="/g/${guildId?c}/events/${event.id?c}/add-user" class="inline-fields">
        <select name="userId" id="addable-members-select" required>
          <option value="" disabled selected>読み込み中...</option>
        </select>
        <button type="submit" class="button secondary">追加</button>
      </form>
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
      <div class="party-card">
        <h3>${party.orderNo}軒目: ${party.shopName}</h3>
        <p>会計金額: ${party.totalAmountText}<#if party.receivedTotalText??> / 受領合計: ${party.receivedTotalText}</#if></p>
        <#if party.note??>
          <p class="note">${party.note}</p>
        </#if>

        <#if party.attendance?size gt 0>
          <table class="attendance-table">
            <thead><tr><th>参加者</th><th>出欠</th><#if canManage><th></th></#if></tr></thead>
            <tbody>
              <#list party.attendance as a>
                <tr>
                  <td>${a.name}</td>
                  <td>
                    <#if a.status == "PRESENT">✅ 出席
                    <#elseif a.status == "ABSENT">❌ 欠席
                    <#else>❔ 未確認
                    </#if>
                  </td>
                  <#if canManage>
                    <td>
                      <form method="post" action="/g/${guildId?c}/parties/${party.id?c}/attendance" class="inline-form">
                        <input type="hidden" name="userId" value="${a.userId?c}">
                        <input type="hidden" name="status" value="PRESENT">
                        <button type="submit" class="button small">出席</button>
                      </form>
                      <form method="post" action="/g/${guildId?c}/parties/${party.id?c}/attendance" class="inline-form">
                        <input type="hidden" name="userId" value="${a.userId?c}">
                        <input type="hidden" name="status" value="ABSENT">
                        <button type="submit" class="button danger small">欠席</button>
                      </form>
                      <#if a.status??>
                        <form method="post" action="/g/${guildId?c}/parties/${party.id?c}/attendance" class="inline-form">
                          <input type="hidden" name="userId" value="${a.userId?c}">
                          <input type="hidden" name="status" value="CLEAR">
                          <button type="submit" class="button secondary small">未確認に戻す</button>
                        </form>
                      </#if>
                    </td>
                  </#if>
                </tr>
              </#list>
            </tbody>
          </table>
          <#if canManage>
            <form method="post" action="/g/${guildId?c}/parties/${party.id?c}/attendance/mark-all-present" class="inline-form">
              <button type="submit" class="button secondary small">全員を出席にする</button>
            </form>
          </#if>
        </#if>

        <#if canManage>
          <table class="payment-table">
            <thead><tr><th>支払者</th><th>金額</th><th>記録者</th><th>受領日時</th></tr></thead>
            <tbody>
              <#list party.payments as payment>
                <tr>
                  <td>${payment.payerName}</td>
                  <td>${payment.amountText}</td>
                  <td>${payment.recordedByName}</td>
                  <td>${payment.receivedAt}</td>
                </tr>
              </#list>
            </tbody>
          </table>

          <details class="edit-details">
            <summary>お店情報を編集</summary>
            <form method="post" action="/g/${guildId?c}/parties/${party.id?c}/edit" class="inline-fields">
              <input type="text" name="shopName" value="${party.shopName}" required maxlength="120">
              <input type="number" name="totalAmount" min="0" value="${party.totalAmount!""}" placeholder="会計金額">
              <button type="submit" class="button secondary">更新</button>
            </form>
          </details>

          <details class="edit-details">
            <summary>受領記録を追加</summary>
            <form method="post" action="/g/${guildId?c}/parties/${party.id?c}/payments" class="inline-fields">
              <select name="payerUserId" required>
                <#list participants as p>
                  <option value="${p.id?c}">${p.name}</option>
                </#list>
              </select>
              <input type="number" name="amount" min="1" placeholder="受領金額" required>
              <button type="submit" class="button secondary">登録</button>
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
