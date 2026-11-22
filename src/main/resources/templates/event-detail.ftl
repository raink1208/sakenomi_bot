<#assign nav = "events">
<#include "_header.ftl">
<section class="event-page" data-tabs data-tab="shops">
  <a class="back-link" href="/g/${guildId?c}/events<#if !event.isRecruiting>?status=archived</#if>">
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 6l-6 6 6 6"/></svg>
    イベント一覧
  </a>

  <div class="event-hero">
    <div class="event-hero-main">
      <div class="eyebrow">
        <span class="eyebrow-number">第${event.number}回</span>
        <#if event.isRecruiting>
          <span class="badge badge-recruit">募集中</span>
        <#else>
          <span class="badge badge-archive">アーカイブ済</span>
        </#if>
      </div>
      <h1 class="event-title">${event.name}</h1>
      <dl class="event-meta">
        <div class="event-meta-item">
          <dt>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></svg>
            <span class="event-meta-label">開催日</span>
          </dt>
          <dd>${event.dateLabel!"未定"}</dd>
        </div>
        <div class="event-meta-item">
          <dt>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 21s-7-6.2-7-11a7 7 0 0 1 14 0c0 4.8-7 11-7 11z"/><circle cx="12" cy="10" r="2.5"/></svg>
            <span class="event-meta-label">エリア</span>
          </dt>
          <dd>${event.area}</dd>
        </div>
        <div class="event-meta-item">
          <dt>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></svg>
            <span class="event-meta-label">幹事</span>
          </dt>
          <dd>${event.organizerName}</dd>
        </div>
        <div class="event-meta-item">
          <dt>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="9" cy="8" r="3.5"/><path d="M2.5 20a6.5 6.5 0 0 1 13 0"/><path d="M16 4.5a3.5 3.5 0 0 1 0 7"/><path d="M18 14a6 6 0 0 1 3.5 6"/></svg>
            <span class="event-meta-label">参加</span>
          </dt>
          <dd>${event.participantCount!""}</dd>
        </div>
      </dl>
    </div>

    <div class="event-hero-actions">
      <#if event.isRecruiting>
        <#if isParticipant>
          <#if !isOrganizer>
            <form method="post" action="/g/${guildId?c}/events/${event.id?c}/leave" class="inline-form">
              <button type="submit" class="button danger secondary">参加をキャンセル</button>
            </form>
          </#if>
          <span class="joined-pill">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 12l5 5 9-10"/></svg>
            参加中
          </span>
        <#else>
          <form method="post" action="/g/${guildId?c}/events/${event.id?c}/join" class="inline-form">
            <button type="submit" class="button">参加する</button>
          </form>
        </#if>
      </#if>
      <#if canManage>
        <button type="button" class="icon-button manage-toggle" data-manage-toggle aria-expanded="false" aria-controls="manage-panel" aria-label="管理メニュー">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><circle cx="5" cy="12" r="1.8"/><circle cx="12" cy="12" r="1.8"/><circle cx="19" cy="12" r="1.8"/></svg>
        </button>
      </#if>
    </div>
  </div>

  <div class="segmented mobile-tabs" role="tablist" aria-label="表示の切り替え">
    <button type="button" class="segmented-item" role="tab" aria-selected="true" data-tab-target="shops">お店 ${parties?size}</button>
    <button type="button" class="segmented-item" role="tab" aria-selected="false" data-tab-target="people">参加者 ${participants?size + guests?size}</button>
  </div>

  <div class="event-layout">
    <div class="event-main" data-tab-panel="shops">
      <div class="section-head">
        <h2>お店</h2>
        <span class="section-count">${parties?size}軒</span>
      </div>

      <#if parties?size == 0>
        <p class="empty">まだお店が登録されていません。</p>
      </#if>
      <#list parties as party>
        <article class="party-card" id="party-${party.id?c}">
          <div class="party-card-head">
            <div class="party-card-title">
              <span class="order-chip">${party.orderNo}軒目</span>
              <h3>${party.shopName}</h3>
            </div>
            <#if canManage && party.attendance?size gt 0>
              <a class="button secondary small" href="/g/${guildId?c}/parties/${party.id?c}/check-in">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="3"/><path d="M8 12l3 3 5-6"/></svg>
                受付モード
              </a>
            </#if>
          </div>

          <div class="stat-grid">
            <div class="stat">
              <div class="stat-label">会計金額</div>
              <div class="stat-value">${party.totalAmountText}</div>
            </div>
            <div class="stat">
              <div class="stat-label">受領合計</div>
              <div class="stat-value">${party.receivedTotalText!"-"}</div>
            </div>
            <#if party.diffText??>
              <#assign diff = party.receivedTotal - party.totalAmount>
              <div class="stat <#if diff lt 0>stat-negative<#else>stat-positive</#if>">
                <div class="stat-label">差額<#if diff lt 0> · 不足<#elseif diff gt 0> · 超過</#if></div>
                <div class="stat-value">${party.diffText}</div>
              </div>
            <#else>
              <div class="stat">
                <div class="stat-label">差額</div>
                <div class="stat-value">-</div>
              </div>
            </#if>
          </div>

          <#if party.note??>
            <p class="note">${party.note}</p>
          </#if>

          <#if party.attendance?size gt 0>
            <div class="party-progress">
              <span><b>${party.presentCount}</b> / ${party.attendance?size}人 出席</span>
              <div class="meter" aria-hidden="true"><span style="width: ${(party.presentCount * 100 / party.attendance?size)?c}%"></span></div>
            </div>

            <#if canManage>
              <form method="post" action="/g/${guildId?c}/parties/${party.id?c}/attendance-payments">
                <div class="att-list">
                  <div class="att-head" aria-hidden="true">
                    <span>名前</span>
                    <span>出欠</span>
                    <span>受領金額</span>
                  </div>
                  <#list party.attendance as a>
                    <div class="att-row" data-att-row>
                      <span class="att-name">${a.name}</span>
                      <div class="status-toggle-group" role="radiogroup" aria-label="${a.name}の出欠">
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
                      <label class="amount-field">
                        <span class="amount-unit">¥</span>
                        <input type="number" name="amount_${a.userId?c}" min="0" step="100" value="<#if a.receivedAmount??>${a.receivedAmount?c}</#if>" placeholder="0" inputmode="numeric" aria-label="${a.name}の受領金額" data-amount>
                      </label>
                    </div>
                  </#list>
                </div>
                <div class="att-actions">
                  <div class="att-quick">
                    <button type="button" class="link-button" data-mark-all-present>全員を出席にする</button>
                    <#if party.totalAmount??>
                      <button type="button" class="link-button" data-fill-even data-total="${party.totalAmount?c}">出席者で割り勘して入力</button>
                    </#if>
                  </div>
                  <button type="submit" class="button">出欠・受領金額を保存</button>
                </div>
              </form>
            <#else>
              <ul class="att-list att-list-readonly">
                <#list party.attendance as a>
                  <li class="att-row">
                    <span class="att-name">${a.name}</span>
                    <span class="status-label status-${(a.status!"UNKNOWN")?lower_case}"><span class="status-dot" aria-hidden="true"></span>${a.statusLabel}</span>
                    <span class="att-amount">${a.receivedAmountText}</span>
                  </li>
                </#list>
              </ul>
            </#if>
          </#if>

          <#if canManage>
            <details class="edit-details">
              <summary>お店情報を編集</summary>
              <form method="post" action="/g/${guildId?c}/parties/${party.id?c}/edit" class="inline-fields">
                <input type="text" name="shopName" value="${party.shopName}" required maxlength="120" aria-label="店名">
                <input type="number" name="totalAmount" min="0" value="<#if party.totalAmount??>${party.totalAmount?c}</#if>" placeholder="会計金額" aria-label="会計金額">
                <button type="submit" class="button secondary">更新</button>
              </form>
              <form method="post" action="/g/${guildId?c}/parties/${party.id?c}/delete" class="inline-form" onsubmit="return confirm('このお店を削除しますか?出欠・受領記録も削除されます。');">
                <button type="submit" class="button danger secondary small">このお店を削除</button>
              </form>
            </details>
          </#if>
        </article>
      </#list>

      <#if canManage>
        <section class="add-card" aria-labelledby="add-party-heading">
          <h3 id="add-party-heading">お店を追加</h3>
          <form method="post" action="/g/${guildId?c}/events/${event.id?c}/parties" class="field-row">
            <label class="field field-grow">
              店名
              <input type="text" name="shopName" placeholder="例: 居酒屋 〇〇" required maxlength="120">
            </label>
            <label class="field">
              軒目(任意)
              <input type="number" name="orderNo" min="1" placeholder="${parties?size + 1}">
            </label>
            <label class="field">
              会計金額(任意)
              <input type="number" name="totalAmount" min="0" placeholder="0">
            </label>
            <button type="submit" class="button">追加</button>
          </form>
        </section>
      </#if>
    </div>

    <aside class="event-side">
      <section class="side-card" data-tab-panel="people" aria-labelledby="participants-heading">
        <div class="side-card-head">
          <h2 id="participants-heading">参加者</h2>
          <span class="section-count">${event.participantCount!""}</span>
        </div>
        <#if participants?size + guests?size == 0>
          <p class="empty">まだ参加者がいません。</p>
        <#else>
          <ul class="person-list">
            <#list participants as p>
              <li>
                <span class="person-name">${p.name}</span>
                <#if p.isOrganizer><span class="tag tag-organizer">幹事</span></#if>
              </li>
            </#list>
            <#list guests as g>
              <li>
                <span class="person-name">${g.name}</span>
                <span class="tag">ゲスト</span>
              </li>
            </#list>
          </ul>
        </#if>

        <#if canManage>
          <details class="edit-details" id="edit-guest-details">
            <summary>ゲストを編集</summary>
            <p class="field-hint">Discord を使っていない参加者をゲストとして登録できます。削除しても出欠・受領記録は残り、同じ名前で再追加すると元に戻ります。</p>
            <form method="post" action="/g/${guildId?c}/events/${event.id?c}/guests" class="inline-fields">
              <input type="text" name="name" placeholder="ゲスト名" required maxlength="80" aria-label="ゲスト名">
              <button type="submit" class="button secondary">追加</button>
            </form>
            <#list guests as g>
              <div class="inline-fields">
                <form method="post" action="/g/${guildId?c}/events/${event.id?c}/guests/${g.id?c}/rename" class="inline-fields">
                  <input type="text" name="name" value="${g.name}" required maxlength="80" aria-label="ゲスト名">
                  <button type="submit" class="button secondary">名前を変更</button>
                </form>
                <form method="post" action="/g/${guildId?c}/events/${event.id?c}/guests/${g.id?c}/delete" class="inline-form" onsubmit="return confirm('ゲスト「${g.name?js_string}」を削除しますか?');">
                  <button type="submit" class="button danger secondary small">削除</button>
                </form>
              </div>
            </#list>
          </details>
        </#if>

        <#if isAdmin>
          <details class="edit-details" id="edit-participant-details">
            <summary>参加者を編集</summary>
            <form method="post" action="/g/${guildId?c}/events/${event.id?c}/add-user" class="inline-fields">
              <select name="userId" id="addable-members-select" required aria-label="追加するメンバー">
                <option value="" disabled selected>読み込み中...</option>
              </select>
              <button type="submit" class="button secondary">追加</button>
            </form>
            <form method="post" action="/g/${guildId?c}/events/${event.id?c}/remove-user" class="inline-fields">
              <select name="userId" required aria-label="削除するメンバー">
                <option value="" disabled selected>削除するメンバーを選択</option>
                <#list participants as p>
                  <#if !p.isOrganizer>
                    <option value="${p.id?c}">${p.name}</option>
                  </#if>
                </#list>
              </select>
              <button type="submit" class="button danger secondary">削除</button>
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
      </section>

      <#if canManage>
        <div class="manage" data-manage>
          <button type="button" class="manage-backdrop" data-manage-close tabindex="-1" aria-label="管理メニューを閉じる"></button>
          <section class="side-card manage-panel" id="manage-panel" aria-labelledby="manage-heading">
            <h2 id="manage-heading">管理</h2>
            <a class="menu-item" href="/g/${guildId?c}/events/${event.id?c}/edit">
              イベントを編集
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 6l6 6-6 6"/></svg>
            </a>
            <details class="menu-details" id="transfer-organizer-details">
              <summary class="menu-item">
                幹事を変更
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M6 9l6 6 6-6"/></svg>
              </summary>
              <form method="post" action="/g/${guildId?c}/events/${event.id?c}/transfer-organizer" class="inline-fields">
                <select name="userId" id="organizer-candidates-select" required aria-label="新しい幹事">
                  <option value="" disabled selected>読み込み中...</option>
                </select>
                <button type="submit" class="button secondary">変更</button>
              </form>
            </details>
            <#if event.isRecruiting>
              <form method="post" action="/g/${guildId?c}/events/${event.id?c}/archive">
                <button type="submit" class="menu-item">アーカイブにする</button>
              </form>
            <#else>
              <form method="post" action="/g/${guildId?c}/events/${event.id?c}/reopen">
                <button type="submit" class="menu-item">アーカイブを解除</button>
              </form>
            </#if>

            <#if isAdmin>
              <div class="manage-admin">
                <span class="manage-admin-label">管理者のみ</span>
                <form method="post" action="/g/${guildId?c}/events/${event.id?c}/renumber" class="renumber-form">
                  <input type="number" name="number" min="1" placeholder="新しい第n回" required aria-label="新しい第n回">
                  <button type="submit" class="button secondary">採番修正</button>
                </form>
                <a class="button danger secondary" href="/g/${guildId?c}/events/${event.id?c}/delete">イベントを削除</a>
              </div>
            </#if>
          </section>
        </div>
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
    </aside>
  </div>
</section>
<#include "_footer.ftl">
