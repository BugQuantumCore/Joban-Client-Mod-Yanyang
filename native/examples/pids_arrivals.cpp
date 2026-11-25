#include <mtr/script.hpp>
struct ArrivalState {};
struct ArrivalDisplay : mtr::PidsScript<ArrivalState> {
    static constexpr auto ID="example:arrivals";
    void render(mtr::PidsContext& ctx,ArrivalState&,const mtr::Pids& pids) override {
        for(int row=0;row<pids.rows();row++) {
            if(pids.is_row_hidden(row)) continue;
            // Zero-copy: keep the snapshot StrView and pass (data, len) to
            // Text::text directly — the old .str() built one heap std::string
            // per row on every frame.
            mtr::StrView text=pids.get_custom_message(row);
            if(text.empty() && row<pids.arrivals().size()) text=pids.arrivals().at(row).destination();
            if(!text.empty()) mtr::Text::create().text(text.data,text.len).pos(2,row*10.0+2).size(pids.width()-4,9).draw(ctx);
        }
    }
};
MTR_REGISTER_PIDS_SCRIPT(ArrivalDisplay)
